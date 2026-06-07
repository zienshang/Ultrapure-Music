from fastapi import APIRouter, Query

from app.schemas.playlist import YouTubePlaylistResponse
from app.schemas.track import TrackResponse
from app.services.recommendation_service import recommendation_service
from app.services.youtube_extractor import youtube_extractor
from app.services.youtube_personalization_service import youtube_personalization_service
from app.utils.logger import get_logger

router = APIRouter()
logger = get_logger(__name__)


@router.get("", response_model=list[TrackResponse])
async def get_recommendations(
    user_id: int = Query(1),
    limit: int = Query(20, ge=1, le=50),
):
    """
    'For you' feed personalized from the user's listening history and favorites.
    """
    items = await recommendation_service.get_recommendations(user_id, limit=limit)
    return [TrackResponse(**item) for item in items]


@router.get("/youtube", response_model=list[TrackResponse])
async def get_youtube_recommendations(
    access_token: str = Query(..., description="YouTube OAuth access token"),
    limit: int = Query(20, ge=1, le=50),
):
    """
    Personalized feed built from the user's YouTube account — recent uploads from
    the channels they are subscribed to.
    """
    items = await youtube_personalization_service.get_account_feed(access_token, limit=limit)
    return [TrackResponse(**item) for item in items]


@router.get("/youtube-playlists", response_model=list[YouTubePlaylistResponse])
async def get_youtube_public_playlists(
    access_token: str = Query(..., description="YouTube OAuth access token"),
    q: str = Query("nhạc hay playlist", description="Search query for public playlists"),
    limit: int = Query(15, ge=1, le=25),
):
    """
    Public playlists made by YouTube users ('Danh sách từ mọi người tạo').
    """
    items = await youtube_personalization_service.search_public_playlists(
        access_token, query=q, limit=limit,
    )
    return [YouTubePlaylistResponse(**item) for item in items]


@router.get("/youtube-playlists/{playlist_id}/items", response_model=list[TrackResponse])
async def get_youtube_playlist_items(
    playlist_id: str,
    access_token: str = Query(..., description="YouTube OAuth access token"),
    limit: int = Query(50, ge=1, le=50),
):
    """Fetch items of any public YouTube playlist by its id."""
    items = await youtube_personalization_service.get_playlist_items_public(
        access_token, playlist_id, limit=limit,
    )
    return [TrackResponse(**item) for item in items]


@router.get("/related", response_model=list[TrackResponse])
async def get_related_songs(
    video_id: str = Query(..., description="YouTube video ID to find related tracks for"),
    limit: int = Query(10, ge=1, le=20),
):
    """
    'Bài hát tương tự' — returns tracks similar to the given video.
    Searches yt-dlp using artist + title as a query and excludes the source video.
    """
    try:
        info = await youtube_extractor.get_metadata(video_id)
        artist = info.get("artist", "")
        title = info.get("title", "")
        # Strip channel suffixes (e.g. "Song - Official MV") for a cleaner query
        base_title = title.split(" - ")[0].split(" | ")[0].strip()
        query = f"{artist} {base_title}" if artist else base_title
        results = await youtube_extractor.search(query, limit=limit + 3)
        filtered = [r for r in results if r.get("youtube_id") != video_id][:limit]
        return [TrackResponse(**r) for r in filtered]
    except Exception as e:
        logger.warning("Related songs failed for %s: %s", video_id, e)
        return []
