from fastapi import APIRouter, Query, HTTPException

from app.db.repositories.history import HistoryRepository
from app.db.repositories.tracks import TrackRepository
from app.schemas.track import TrackDetailResponse
from app.services.youtube_extractor import youtube_extractor
from app.services.stream_service import stream_service
from app.utils.token_utils import create_stream_token

router = APIRouter()


@router.get("/stream", response_model=TrackDetailResponse)
async def get_stream(
    video_id: str = Query(..., description="YouTube video ID"),
    user_id: int | None = Query(None, description="User ID for analytics"),
    quality: int = Query(0, ge=0, le=3, description="Audio quality: 0=auto, 1=low, 2=medium, 3=high"),
):
    info = await youtube_extractor.get_metadata(video_id)
    stream_url = await stream_service.get_stream_url(video_id, quality=quality)

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

    if user_id:
        track = await TrackRepository.get_by_youtube_id(video_id)
        if track:
            await HistoryRepository.add(user_id=user_id, track_id=track["id"])

    stream_token = create_stream_token(video_id, user_id)

    return TrackDetailResponse(
        **info,
        stream_url=stream_url,
        is_favorited=False,
    )


@router.post("/play")
async def track_playback(
    video_id: str = Query(...),
    user_id: int | None = Query(None),
    duration_played: int = 0,
):
    if user_id:
        track = await TrackRepository.get_by_youtube_id(video_id)
        if track:
            await HistoryRepository.add(
                user_id=user_id,
                track_id=track["id"],
                duration_played=duration_played,
            )
    return {"status": "logged"}
