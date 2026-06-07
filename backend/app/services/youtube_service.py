from app.db.repositories.tracks import TrackRepository
from app.services.youtube_extractor import youtube_extractor
from app.services.cache_service import cache_service
from app.utils.logger import get_logger

logger = get_logger(__name__)


class YouTubeService:

    @staticmethod
    async def search_and_save(query: str, limit: int = 10) -> list[dict]:
        results = await youtube_extractor.search(query, limit=limit)
        saved = []
        for item in results:
            try:
                track = await TrackRepository.find_or_create(
                    youtube_id=item["youtube_id"],
                    title=item["title"],
                    artist=item["artist"],
                    duration=item["duration"],
                    thumbnail_url=item["thumbnail_url"],
                    webpage_url=item["webpage_url"],
                    uploaded_at=item["uploaded_at"],
                    view_count=item["view_count"],
                )
                await cache_service.set_track_metadata(item["youtube_id"], item)
                saved.append(track)
            except Exception as e:
                logger.warning("Failed to save track %s: %s", item["youtube_id"], e)
                saved.append(item)
        return saved

    @staticmethod
    async def get_track_info(youtube_id: str) -> dict:
        cached = await cache_service.get_track_metadata(youtube_id)
        if cached:
            return cached

        info = await youtube_extractor.get_metadata(youtube_id)
        await cache_service.set_track_metadata(youtube_id, info)

        try:
            await TrackRepository.find_or_create(
                youtube_id=info["youtube_id"],
                title=info["title"],
                artist=info["artist"],
                duration=info["duration"],
                thumbnail_url=info["thumbnail_url"],
                webpage_url=info["webpage_url"],
                uploaded_at=info["uploaded_at"],
                view_count=info["view_count"],
            )
        except Exception as e:
            logger.warning("Failed to save track metadata: %s", e)

        return info

    @staticmethod
    async def get_stream_url(youtube_id: str) -> str:
        from app.services.stream_service import stream_service
        return await stream_service.get_stream_url(youtube_id)


youtube_service = YouTubeService()
