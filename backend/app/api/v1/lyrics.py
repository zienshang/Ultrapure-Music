from fastapi import APIRouter, Query
from pydantic import BaseModel
import httpx

from app.utils.logger import get_logger

router = APIRouter()
logger = get_logger(__name__)

LRCLIB_URL = "https://lrclib.net/api/get"


class LyricsResponse(BaseModel):
    track_name: str = ""
    artist_name: str = ""
    synced_lyrics: str | None = None
    plain_lyrics: str | None = None


@router.get("", response_model=LyricsResponse)
async def get_lyrics(
    track_name: str = Query(..., description="Tên bài hát"),
    artist_name: str = Query("", description="Tên nghệ sĩ (tăng độ chính xác)"),
    duration: int = Query(0, description="Thời lượng giây (tăng độ chính xác)"),
):
    """
    Proxy cho LRCLib (lrclib.net) — trả lời bài hát dạng synced LRC và plain text.
    Không cần API key, hoàn toàn miễn phí.
    """
    params: dict = {"track_name": track_name}
    if artist_name:
        params["artist_name"] = artist_name
    if duration > 0:
        params["duration"] = duration

    try:
        async with httpx.AsyncClient(timeout=8.0) as client:
            resp = await client.get(LRCLIB_URL, params=params)

        if resp.status_code == 200:
            data = resp.json()
            return LyricsResponse(
                track_name=data.get("trackName", ""),
                artist_name=data.get("artistName", ""),
                synced_lyrics=data.get("syncedLyrics"),
                plain_lyrics=data.get("plainLyrics"),
            )
        logger.debug("LRCLib %d for '%s' by '%s'", resp.status_code, track_name, artist_name)

    except Exception as e:
        logger.warning("Lyrics fetch failed for '%s': %s", track_name, e)

    return LyricsResponse()
