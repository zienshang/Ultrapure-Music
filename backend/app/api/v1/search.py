import time

from fastapi import APIRouter, Query

from app.schemas.track import TrackResponse, TrackSearchResponse
from app.services.youtube_service import youtube_service
from app.utils.logger import get_logger

router = APIRouter()
logger = get_logger(__name__)

_search_cache: dict[str, dict] = {}
_SEARCH_CACHE_TTL = 300

_trending_cache: dict = {"data": None, "timestamp": 0.0}
_TRENDING_CACHE_TTL = 1800  # 30 minutes
_DEFAULT_TRENDING_QUERY = "nhạc trẻ hay nhất"


@router.get("/trending", response_model=TrackSearchResponse)
async def trending_tracks(
    q: str = Query(_DEFAULT_TRENDING_QUERY, description="Trending query seed"),
    limit: int = Query(20, ge=1, le=50, description="Number of results"),
):
    """
    A lightweight 'Trending Now' feed sourced via yt-dlp (no YouTube Data API key
    required). Cached for 30 minutes to avoid hammering yt-dlp.
    """
    use_default = q.strip() == _DEFAULT_TRENDING_QUERY
    if use_default:
        cached = _trending_cache["data"]
        if cached and time.time() - _trending_cache["timestamp"] < _TRENDING_CACHE_TTL:
            logger.debug("Trending cache hit")
            return cached

    results = await youtube_service.search_and_save(q, limit=limit)
    track_responses = [TrackResponse(**item) for item in results]
    data = TrackSearchResponse(
        query=q,
        results=track_responses,
        total=len(track_responses),
        page=1,
        page_size=limit,
    )

    if use_default and track_responses:
        _trending_cache["data"] = data
        _trending_cache["timestamp"] = time.time()

    logger.info("Trending '%s' returned %d results", q, len(track_responses))
    return data


@router.get("", response_model=TrackSearchResponse)
async def search_tracks(
    q: str = Query(..., min_length=1, max_length=200, description="Search query"),
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(10, ge=1, le=50, description="Results per page"),
):
    cache_key = f"{q.lower().strip()}:{page}:{page_size}"
    cached = _search_cache.get(cache_key)
    if cached and time.time() - cached["timestamp"] < _SEARCH_CACHE_TTL:
        logger.debug("Search cache hit for '%s'", q)
        return cached["data"]

    results = await youtube_service.search_and_save(q, limit=page * page_size)

    total = len(results)
    start = (page - 1) * page_size
    end = start + page_size
    page_results = results[start:end]

    track_responses = [TrackResponse(**item) for item in page_results]

    data = TrackSearchResponse(
        query=q,
        results=track_responses,
        total=total,
        page=page,
        page_size=page_size,
    )

    # Only cache non-empty results to avoid storing failures
    if total > 0:
        _search_cache[cache_key] = {"data": data, "timestamp": time.time()}

    logger.info("Search '%s' returned %d results (page %d)", q, total, page)
    return data
