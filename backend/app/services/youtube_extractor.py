from app.utils.yt_dlp_helper import ytdl_helper
from app.utils.logger import get_logger
from app.utils.exceptions import ServiceUnavailableException, NotFoundException

logger = get_logger(__name__)


class YouTubeExtractor:

    @staticmethod
    async def search(query: str, limit: int = 10) -> list[dict]:
        try:
            entries = ytdl_helper.search(query, limit=limit)
            results = []
            for entry in entries:
                if not entry or not entry.get("id"):
                    continue
                try:
                    results.append(YouTubeExtractor._normalize_track(entry))
                except Exception as norm_err:
                    logger.debug("Skipping entry %s: %s", entry.get("id"), norm_err)
            logger.info("Search '%s' returned %d results", query, len(results))
            return results
        except Exception as e:
            logger.error("Search failed for '%s': %s", query, str(e))
            raise ServiceUnavailableException(f"YouTube search failed: {str(e)}")

    @staticmethod
    async def get_metadata(video_id: str) -> dict:
        try:
            info = ytdl_helper.get_metadata(video_id)
            if not info or not info.get("id"):
                raise NotFoundException(f"Video {video_id} not found")
            return YouTubeExtractor._normalize_track(info)
        except NotFoundException:
            raise
        except Exception as e:
            logger.error("Metadata fetch failed for %s: %s", video_id, str(e))
            raise ServiceUnavailableException(f"Failed to fetch metadata: {str(e)}")

    # Maps audioQuality int (0–3) from the Android app to yt-dlp format strings.
    _QUALITY_FORMATS: dict[int, str] = {
        0: "bestaudio/best",                        # auto
        1: "worstaudio/worst",                      # low (~48 kbps)
        2: "bestaudio[abr<=128]/best[abr<=128]",    # medium (~128 kbps)
        3: "bestaudio[ext=m4a]/bestaudio/best",     # high (prefer m4a, best available)
    }

    @staticmethod
    async def get_stream_url(video_id: str, quality: int = 0) -> str:
        format_str = YouTubeExtractor._QUALITY_FORMATS.get(quality, "bestaudio/best")
        try:
            url = ytdl_helper.get_stream_url(video_id, quality=format_str)
            if not url:
                raise NotFoundException(f"Stream URL not available for {video_id}")
            return url
        except NotFoundException:
            raise
        except Exception as e:
            logger.error("Stream URL fetch failed for %s: %s", video_id, str(e))
            raise ServiceUnavailableException(f"Failed to get stream URL: {str(e)}")

    @staticmethod
    async def get_thumbnail_url(video_id: str) -> str:
        return ytdl_helper.get_thumbnail_url(video_id)

    @staticmethod
    def _normalize_track(info: dict) -> dict:
        video_id = info.get("id") or ""

        # Thumbnail: flat entries may provide a list; pick the best one
        thumbnail = info.get("thumbnail") or ""
        if not thumbnail:
            thumbnails = info.get("thumbnails") or []
            if thumbnails:
                # Prefer hqdefault (4th quality) or the last entry
                hq = next(
                    (t.get("url") for t in thumbnails if "hqdefault" in (t.get("url") or "")),
                    None,
                )
                thumbnail = hq or thumbnails[-1].get("url", "")
        if not thumbnail and video_id:
            thumbnail = f"https://i.ytimg.com/vi/{video_id}/hqdefault.jpg"

        return {
            "youtube_id": video_id,
            "title": info.get("title") or "Unknown",
            "artist": (
                info.get("uploader")
                or info.get("channel")
                or info.get("uploader_id")
                or ""
            ),
            "duration": int(info.get("duration") or 0),
            "thumbnail_url": thumbnail,
            "webpage_url": (
                info.get("webpage_url")
                or info.get("url")
                or (f"https://www.youtube.com/watch?v={video_id}" if video_id else "")
            ),
            "uploaded_at": info.get("upload_date") or "",
            "view_count": int(info.get("view_count") or 0),
        }


youtube_extractor = YouTubeExtractor()
