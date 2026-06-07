from apscheduler.schedulers.asyncio import AsyncIOScheduler

from app.config.settings import settings
from app.tasks.cleanup_cache import cleanup_cache
from app.tasks.youtube_sync import auto_sync_playlists
from app.utils.logger import get_logger

logger = get_logger(__name__)

scheduler = AsyncIOScheduler()


async def start_scheduler() -> None:
    if scheduler.running:
        return

    sync_interval = max(settings.sync_interval_hours, 1)
    cache_interval = max(settings.cache_cleanup_interval_hours, 1)

    scheduler.add_job(
        auto_sync_playlists,
        "interval",
        hours=sync_interval,
        id="auto_sync_playlists",
        replace_existing=True,
    )

    scheduler.add_job(
        cleanup_cache,
        "interval",
        hours=cache_interval,
        id="cleanup_cache",
        replace_existing=True,
    )

    scheduler.start()
    logger.info(
        "Scheduler started: sync every %dh, cache cleanup every %dh",
        sync_interval,
        cache_interval,
    )


async def stop_scheduler() -> None:
    if scheduler.running:
        scheduler.shutdown(wait=False)
        logger.info("Scheduler stopped")
