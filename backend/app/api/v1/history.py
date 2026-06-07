from fastapi import APIRouter, Query

from app.db.repositories.history import HistoryRepository
from app.schemas.track import TrackResponse
from app.services.youtube_service import youtube_service
from app.utils.logger import get_logger

router = APIRouter()
logger = get_logger(__name__)

_POPULAR_FALLBACK_QUERY = "nhạc trẻ thịnh hành"


@router.get("", response_model=dict)
async def get_history(user_id: int = 1, limit: int = 50, offset: int = 0):
    entries = await HistoryRepository.get_by_user(user_id, limit=limit, offset=offset)
    total = await HistoryRepository.count_for_user(user_id)
    return {
        "entries": [TrackResponse(**e) for e in entries],
        "total": total,
        "limit": limit,
        "offset": offset,
    }


@router.get("/recent", response_model=list[TrackResponse])
async def get_recent_tracks(user_id: int = 1, limit: int = 10):
    entries = await HistoryRepository.get_recent_tracks(user_id, limit=limit)
    return [TrackResponse(**e) for e in entries]


@router.get("/most-played", response_model=list[dict])
async def get_most_played(user_id: int = 1, limit: int = 20):
    return await HistoryRepository.get_most_played(user_id, limit=limit)


@router.get("/popular", response_model=list[TrackResponse])
async def get_popular_tracks(limit: int = Query(20, ge=1, le=50)):
    """
    'Thịnh hành với người nghe' — tracks most played across the whole listener
    community. Falls back to a popular yt-dlp feed when there isn't enough
    listening history yet.
    """
    global_top = await HistoryRepository.get_global_most_played(limit=limit)
    if len(global_top) >= 4:
        return [TrackResponse(**t) for t in global_top]

    logger.info("Not enough play history (%d); using trending fallback", len(global_top))
    fallback = await youtube_service.search_and_save(_POPULAR_FALLBACK_QUERY, limit=limit)
    return [TrackResponse(**t) for t in fallback]


@router.post("")
async def record_playback(
    youtube_id: str = Query(...),
    user_id: int = 1,
    duration_played: int = 0,
):
    from app.db.repositories.tracks import TrackRepository
    track = await TrackRepository.get_by_youtube_id(youtube_id)
    if not track:
        await TrackRepository.create(youtube_id=youtube_id, title=youtube_id)
        track = await TrackRepository.get_by_youtube_id(youtube_id)
    entry = await HistoryRepository.add(user_id, track["id"], duration_played)
    return {"status": "recorded", "history_id": entry["id"]}


@router.delete("")
async def clear_history(user_id: int = 1):
    deleted = await HistoryRepository.clear_for_user(user_id)
    return {"status": "cleared" if deleted else "empty"}
