import asyncio

from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from google.oauth2.credentials import Credentials

from app.db.repositories.playlists import PlaylistRepository
from app.db.repositories.tracks import TrackRepository
from app.utils.logger import get_logger

logger = get_logger(__name__)


def _raise_friendly(e: HttpError) -> None:
    """Convert googleapiclient HttpError into a human-readable exception."""
    status = e.resp.status
    reason = ""
    message = ""
    try:
        import json
        body = json.loads(e.content)
        err = body.get("error", {})
        message = err.get("message", "")
        reason = err.get("errors", [{}])[0].get("reason", "")
    except Exception:
        pass

    # Always log the FULL error body — it contains the exact project number
    # and a direct activation URL when the API is not enabled.
    logger.error("YouTube API HttpError %s: %s", status, e.content)

    if status == 403 and reason == "accessNotConfigured":
        raise RuntimeError(
            "YouTube Data API v3 chưa được bật cho project chứa access token. "
            f"Chi tiết từ Google: {message}"
        )
    if status == 401:
        raise RuntimeError(
            "Access token YouTube không hợp lệ hoặc đã hết hạn. "
            "Hãy ngắt kết nối và kết nối lại tài khoản YouTube."
        )
    if status == 403:
        raise RuntimeError(
            f"Không có quyền truy cập YouTube API (403 {reason}). "
            "Hãy kiểm tra OAuth scope và thử lại."
        )
    raise RuntimeError(f"YouTube API lỗi {status}: {e}")


def _best_thumbnail(thumbnails: dict) -> str | None:
    """Pick the highest-resolution thumbnail available."""
    for key in ("maxres", "standard", "high", "medium", "default"):
        t = thumbnails.get(key)
        if t and t.get("url"):
            return t["url"]
    return None


class YouTubeSyncService:

    @staticmethod
    def _build_client(access_token: str):
        """Build an authenticated YouTube API client.
        cache_discovery=False suppresses the oauth2client file_cache warning."""
        creds = Credentials(token=access_token)
        return build("youtube", "v3", credentials=creds, cache_discovery=False)

    # ── Low-level fetchers (all blocking calls wrapped in asyncio.to_thread) ──

    @staticmethod
    async def fetch_user_playlists(access_token: str) -> list[dict]:
        youtube = YouTubeSyncService._build_client(access_token)
        request = youtube.playlists().list(
            part="snippet,contentDetails",
            mine=True,
            maxResults=50,
        )
        try:
            response = await asyncio.to_thread(request.execute)
        except HttpError as e:
            _raise_friendly(e)
        return response.get("items", [])

    @staticmethod
    async def fetch_playlist_items(access_token: str, playlist_id: str) -> list[dict]:
        youtube = YouTubeSyncService._build_client(access_token)
        items: list[dict] = []
        request = youtube.playlistItems().list(
            part="snippet,contentDetails",
            playlistId=playlist_id,
            maxResults=50,
        )
        try:
            while request:
                response = await asyncio.to_thread(request.execute)
                items.extend(response.get("items", []))
                request = youtube.playlistItems().list_next(request, response)
        except HttpError as e:
            _raise_friendly(e)
        return items

    # ── High-level sync operations ────────────────────────────────────────────

    @staticmethod
    async def sync_all_playlists(user_id: int, access_token: str) -> dict:
        stats = {"synced": 0, "skipped": 0, "tracks_added": 0}

        remote_playlists = await YouTubeSyncService.fetch_user_playlists(access_token)
        logger.info("Found %d playlists on YouTube for user %d", len(remote_playlists), user_id)

        for pl in remote_playlists:
            yt_id       = pl["id"]
            title       = pl["snippet"]["title"]
            description = pl["snippet"].get("description", "")

            existing = await PlaylistRepository.get_by_youtube_id(yt_id)
            if existing:
                await PlaylistRepository.update(
                    existing["id"],
                    name=title,
                    description=description,
                    youtube_playlist_id=yt_id,
                )
                local_playlist_id  = existing["id"]
                stats["skipped"] += 1
            else:
                new_pl = await PlaylistRepository.create(
                    user_id=user_id,
                    name=title,
                    description=description,
                    youtube_playlist_id=yt_id,
                )
                local_playlist_id = new_pl["id"]
                stats["synced"] += 1

            items = await YouTubeSyncService.fetch_playlist_items(access_token, yt_id)

            current_tracks    = await PlaylistRepository.get_tracks(local_playlist_id)
            existing_track_ids = {t["youtube_id"] for t in current_tracks if t.get("youtube_id")}

            for item in items:
                snippet     = item.get("snippet", {})
                resource_id = snippet.get("resourceId", {})
                video_id    = resource_id.get("videoId")
                if not video_id or video_id in existing_track_ids:
                    continue

                track = await TrackRepository.find_or_create(
                    youtube_id    = video_id,
                    title         = snippet.get("title", "Unknown"),
                    artist        = snippet.get("videoOwnerChannelTitle",
                                               snippet.get("channelTitle", "")),
                    thumbnail_url = _best_thumbnail(snippet.get("thumbnails", {})),
                )
                await PlaylistRepository.add_track(local_playlist_id, track["id"])
                stats["tracks_added"] += 1

            await PlaylistRepository.update(local_playlist_id, is_synced=1)

        logger.info("Sync complete for user %d: %s", user_id, stats)
        return stats

    @staticmethod
    async def sync_single_playlist(
        user_id: int,
        access_token: str,
        youtube_playlist_id: str,
    ) -> dict:
        stats = {"tracks_added": 0, "tracks_removed": 0}

        items = await YouTubeSyncService.fetch_playlist_items(access_token, youtube_playlist_id)

        existing = await PlaylistRepository.get_by_youtube_id(youtube_playlist_id)
        if not existing:
            playlist_info = items[0]["snippet"] if items else {}
            new_pl = await PlaylistRepository.create(
                user_id=user_id,
                name=playlist_info.get("title", "Synced Playlist"),
                description=playlist_info.get("description", ""),
                youtube_playlist_id=youtube_playlist_id,
            )
            local_playlist_id = new_pl["id"]
        else:
            local_playlist_id = existing["id"]

        current_tracks    = await PlaylistRepository.get_tracks(local_playlist_id)
        current_youtube_ids = {t["youtube_id"] for t in current_tracks if t.get("youtube_id")}

        remote_video_ids: set[str] = set()
        for item in items:
            snippet     = item.get("snippet", {})
            resource_id = snippet.get("resourceId", {})
            video_id    = resource_id.get("videoId")
            if not video_id:
                continue
            remote_video_ids.add(video_id)

            if video_id in current_youtube_ids:
                continue

            track = await TrackRepository.find_or_create(
                youtube_id    = video_id,
                title         = snippet.get("title", "Unknown"),
                artist        = snippet.get("videoOwnerChannelTitle",
                                           snippet.get("channelTitle", "")),
                thumbnail_url = snippet.get("thumbnails", {}).get("default", {}).get("url"),
            )
            await PlaylistRepository.add_track(local_playlist_id, track["id"])
            stats["tracks_added"] += 1

        for yt_id in (current_youtube_ids - remote_video_ids):
            track = await TrackRepository.get_by_youtube_id(yt_id)
            if track:
                await PlaylistRepository.remove_track(local_playlist_id, track["id"])
                stats["tracks_removed"] += 1

        await PlaylistRepository.update(local_playlist_id, is_synced=1)
        logger.info("Single sync complete for playlist %s: %s", youtube_playlist_id, stats)
        return stats


youtube_sync_service = YouTubeSyncService()
