from app.db.repositories.favorites import FavoriteRepository
from app.db.repositories.tracks import TrackRepository
from app.utils.logger import get_logger

logger = get_logger(__name__)


class FavoriteService:

    @staticmethod
    async def add_favorite(user_id: int, track_id: int) -> dict:
        return await FavoriteRepository.add(user_id, track_id)

    @staticmethod
    async def remove_favorite(user_id: int, track_id: int) -> bool:
        return await FavoriteRepository.remove(user_id, track_id)

    @staticmethod
    async def get_user_favorites(user_id: int) -> list[dict]:
        return await FavoriteRepository.get_by_user(user_id)

    @staticmethod
    async def is_favorited(user_id: int, youtube_id: str) -> bool:
        track = await TrackRepository.get_by_youtube_id(youtube_id)
        if not track:
            return False
        return await FavoriteRepository.is_favorited(user_id, track["id"])

    @staticmethod
    async def toggle_favorite(user_id: int, track_id: int) -> dict:
        is_fav = await FavoriteRepository.is_favorited(user_id, track_id)
        if is_fav:
            await FavoriteRepository.remove(user_id, track_id)
            return {"favorited": False, "track_id": track_id}
        else:
            await FavoriteRepository.add(user_id, track_id)
            return {"favorited": True, "track_id": track_id}


favorite_service = FavoriteService()
