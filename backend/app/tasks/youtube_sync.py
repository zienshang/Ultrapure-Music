from app.services.youtube_sync_service import youtube_sync_service
from app.utils.logger import get_logger

logger = get_logger(__name__)


async def auto_sync_playlists() -> None:
    logger.info("Running scheduled playlist sync...")
    try:
        from app.db.repositories.users import UserRepository
        from app.services.auth_service import AuthService

        users = await UserRepository.get_all()
        for user in users:
            refresh_token = AuthService._refresh_tokens.get(user["id"])
            if refresh_token:
                stats = await youtube_sync_service.sync_all_playlists(
                    user["id"], refresh_token
                )
                logger.info("Auto-sync for user %d: %s", user["id"], stats)
        logger.info("Scheduled playlist sync complete")
    except Exception as e:
        logger.error("Auto-sync failed: %s", str(e))
