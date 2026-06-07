from fastapi import APIRouter, Query, HTTPException

from app.db.repositories.tracks import TrackRepository
from app.schemas.track import TrackResponse
from app.services.favorite_service import favorite_service
from app.utils.exceptions import NotFoundException

router = APIRouter()


@router.get("", response_model=list[TrackResponse])
async def list_favorites(user_id: int = 1):
    favorites = await favorite_service.get_user_favorites(user_id)
    return [TrackResponse(**f) for f in favorites]


@router.post("")
async def add_favorite(youtube_id: str = Query(...), user_id: int = 1):
    track = await TrackRepository.get_by_youtube_id(youtube_id)
    if not track:
        raise HTTPException(status_code=404, detail=f"Track {youtube_id} not found")
    result = await favorite_service.toggle_favorite(user_id, track["id"])
    return result


@router.delete("")
async def remove_favorite(youtube_id: str = Query(...), user_id: int = 1):
    track = await TrackRepository.get_by_youtube_id(youtube_id)
    if not track:
        raise HTTPException(status_code=404, detail=f"Track {youtube_id} not found")
    deleted = await favorite_service.remove_favorite(user_id, track["id"])
    if not deleted:
        raise HTTPException(status_code=404, detail="Favorite not found")
    return {"status": "removed", "youtube_id": youtube_id}


@router.get("/status")
async def check_favorite(youtube_id: str = Query(...), user_id: int = 1):
    is_fav = await favorite_service.is_favorited(user_id, youtube_id)
    return {"youtube_id": youtube_id, "is_favorited": is_fav}


@router.get("/ids")
async def get_favorite_ids(user_id: int = 1):
    from app.db.repositories.favorites import FavoriteRepository
    track_ids = await FavoriteRepository.get_favorite_track_ids(user_id)
    youtube_ids = []
    for tid in track_ids:
        t = await TrackRepository.get_by_id(tid)
        if t:
            youtube_ids.append(t["youtube_id"])
    return {"favorite_ids": youtube_ids}
