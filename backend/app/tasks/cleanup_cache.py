from app.services.stream_service import stream_service
from app.services.cache_service import cache_service
from app.utils.logger import get_logger

logger = get_logger(__name__)


async def cleanup_cache() -> None:
    logger.info("Running scheduled cache cleanup...")
    try:
        expired_streams = await stream_service.clear_expired()
        expired_metadata = await cache_service.clear_expired_metadata()
        logger.info(
            "Cache cleanup: %d expired streams, %d expired metadata entries removed",
            expired_streams,
            expired_metadata,
        )
    except Exception as e:
        logger.error("Cache cleanup failed: %s", str(e))
