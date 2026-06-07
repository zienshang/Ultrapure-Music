import asyncio
import random

from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from google.oauth2.credentials import Credentials

from app.utils.logger import get_logger

logger = get_logger(__name__)


def _best_thumbnail(thumbnails: dict) -> str | None:
    for key in ("maxres", "standard", "high", "medium", "default"):
        t = thumbnails.get(key)
        if t and t.get("url"):
            return t["url"]
    return None


class YouTubePersonalizationService:
    """
    Builds a personalized feed from the signed-in user's YouTube account using
    the `youtube.readonly` scope. Based on the channels the user is subscribed to
    (a reliable signal — watch history / liked videos are blocked by Google's API).
    """

    @staticmethod
    def _client(access_token: str):
        creds = Credentials(token=access_token)
        return build("youtube", "v3", credentials=creds, cache_discovery=False)

    @staticmethod
    async def _subscribed_channel_ids(youtube, max_channels: int = 25) -> list[str]:
        request = youtube.subscriptions().list(
            part="snippet",
            mine=True,
            maxResults=min(max_channels, 50),
            order="relevance",
        )
        response = await asyncio.to_thread(request.execute)
        ids = []
        for item in response.get("items", []):
            cid = item.get("snippet", {}).get("resourceId", {}).get("channelId")
            if cid:
                ids.append(cid)
        return ids

    @staticmethod
    async def _uploads_playlists(youtube, channel_ids: list[str]) -> list[str]:
        if not channel_ids:
            return []
        request = youtube.channels().list(
            part="contentDetails",
            id=",".join(channel_ids[:50]),
            maxResults=50,
        )
        response = await asyncio.to_thread(request.execute)
        uploads = []
        for item in response.get("items", []):
            pid = (
                item.get("contentDetails", {})
                .get("relatedPlaylists", {})
                .get("uploads")
            )
            if pid:
                uploads.append(pid)
        return uploads

    @staticmethod
    async def _playlist_recent(youtube, playlist_id: str, n: int = 3) -> list[dict]:
        request = youtube.playlistItems().list(
            part="snippet",
            playlistId=playlist_id,
            maxResults=min(n, 10),
        )
        response = await asyncio.to_thread(request.execute)
        tracks = []
        for item in response.get("items", []):
            snippet = item.get("snippet", {})
            video_id = snippet.get("resourceId", {}).get("videoId")
            if not video_id:
                continue
            tracks.append({
                "youtube_id": video_id,
                "title": snippet.get("title", "Unknown"),
                "artist": snippet.get("videoOwnerChannelTitle",
                                      snippet.get("channelTitle", "")),
                "duration": 0,
                "thumbnail_url": _best_thumbnail(snippet.get("thumbnails", {})),
                "webpage_url": f"https://www.youtube.com/watch?v={video_id}",
                "uploaded_at": (snippet.get("publishedAt") or "")[:10].replace("-", ""),
                "view_count": 0,
            })
        return tracks

    @classmethod
    async def get_account_feed(cls, access_token: str, limit: int = 20) -> list[dict]:
        try:
            youtube = cls._client(access_token)
            channel_ids = await cls._subscribed_channel_ids(youtube)
            if not channel_ids:
                logger.info("User has no subscriptions; no YouTube account feed")
                return []

            # Sample a handful of channels so quota stays low and the feed varies.
            random.shuffle(channel_ids)
            uploads = await cls._uploads_playlists(youtube, channel_ids[:8])

            per_channel = max(2, (limit // max(1, len(uploads))) + 1)
            results: list[dict] = []
            seen: set[str] = set()

            for pid in uploads:
                try:
                    tracks = await cls._playlist_recent(youtube, pid, n=per_channel)
                except HttpError as e:
                    logger.warning("Uploads fetch failed for %s: %s", pid, e)
                    continue
                for t in tracks:
                    if t["youtube_id"] in seen:
                        continue
                    seen.add(t["youtube_id"])
                    results.append(t)
                if len(results) >= limit:
                    break

            random.shuffle(results)
            logger.info("YouTube account feed: %d tracks from %d channels",
                        len(results), len(uploads))
            return results[:limit]

        except HttpError as e:
            logger.error("YouTube account feed failed: %s", e.content if hasattr(e, "content") else e)
            return []
        except Exception as e:
            logger.error("YouTube account feed error: %s", e)
            return []

    # ── Public playlists (community-made) ─────────────────────────────────────

    @classmethod
    async def search_public_playlists(
        cls,
        access_token: str,
        query: str = "nhạc hay playlist",
        limit: int = 15,
    ) -> list[dict]:
        """
        Search YouTube for public playlists made by other users
        ('Danh sách từ mọi người tạo').
        """
        try:
            youtube = cls._client(access_token)
            req = youtube.search().list(
                part="snippet",
                q=query,
                type="playlist",
                maxResults=min(limit, 25),
                order="relevance",
            )
            resp = await asyncio.to_thread(req.execute)
            playlists: list[dict] = []
            ids: list[str] = []
            for item in resp.get("items", []):
                pid = item.get("id", {}).get("playlistId")
                if not pid:
                    continue
                sn = item.get("snippet", {})
                playlists.append({
                    "youtube_playlist_id": pid,
                    "name": sn.get("title", "Playlist"),
                    "channel": sn.get("channelTitle", ""),
                    "thumbnail_url": _best_thumbnail(sn.get("thumbnails", {})),
                    "track_count": 0,
                })
                ids.append(pid)

            # Enrich with item counts via playlists.list (one call, max 50 ids).
            if ids:
                try:
                    creq = youtube.playlists().list(
                        part="contentDetails",
                        id=",".join(ids[:50]),
                        maxResults=50,
                    )
                    cresp = await asyncio.to_thread(creq.execute)
                    counts = {
                        c["id"]: c.get("contentDetails", {}).get("itemCount", 0)
                        for c in cresp.get("items", [])
                    }
                    for p in playlists:
                        p["track_count"] = counts.get(p["youtube_playlist_id"], 0)
                except HttpError as e:
                    logger.warning("Playlist count fetch failed: %s", e)

            logger.info("Public playlists search '%s': %d results", query, len(playlists))
            return playlists

        except HttpError as e:
            logger.error("Public playlist search failed: %s",
                         e.content if hasattr(e, "content") else e)
            return []
        except Exception as e:
            logger.error("Public playlist search error: %s", e)
            return []

    @classmethod
    async def get_playlist_items_public(
        cls,
        access_token: str,
        playlist_id: str,
        limit: int = 50,
    ) -> list[dict]:
        """Fetch the items of a public YouTube playlist (any playlist by id)."""
        try:
            youtube = cls._client(access_token)
            req = youtube.playlistItems().list(
                part="snippet",
                playlistId=playlist_id,
                maxResults=min(limit, 50),
            )
            resp = await asyncio.to_thread(req.execute)
            tracks: list[dict] = []
            for item in resp.get("items", []):
                sn = item.get("snippet", {})
                vid = sn.get("resourceId", {}).get("videoId")
                if not vid:
                    continue
                tracks.append({
                    "youtube_id": vid,
                    "title": sn.get("title", "Unknown"),
                    "artist": sn.get("videoOwnerChannelTitle",
                                     sn.get("channelTitle", "")),
                    "duration": 0,
                    "thumbnail_url": _best_thumbnail(sn.get("thumbnails", {})),
                    "webpage_url": f"https://www.youtube.com/watch?v={vid}",
                    "uploaded_at": (sn.get("publishedAt") or "")[:10].replace("-", ""),
                    "view_count": 0,
                })
            return tracks
        except HttpError as e:
            logger.error("Playlist items fetch failed for %s: %s",
                         playlist_id, e.content if hasattr(e, "content") else e)
            return []
        except Exception as e:
            logger.error("Playlist items fetch error for %s: %s", playlist_id, e)
            return []


youtube_personalization_service = YouTubePersonalizationService()
