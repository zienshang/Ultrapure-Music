from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel

from app.db.repositories.playlists import PlaylistRepository
from app.schemas.playlist import PlaylistCreate, PlaylistUpdate, PlaylistResponse, PlaylistDetailResponse
from app.schemas.track import TrackResponse
from app.services.playlist_service import playlist_service
from app.services.youtube_extractor import youtube_extractor
from app.services.youtube_sync_service import youtube_sync_service
from app.utils.exceptions import NotFoundException, ConflictException
from app.utils.logger import get_logger

logger = get_logger(__name__)


class ImportFromTextRequest(BaseModel):
    name: str
    tracks_text: str
    user_id: int = 1

router = APIRouter()


@router.post("", response_model=PlaylistResponse, status_code=201)
async def create_playlist(body: PlaylistCreate, user_id: int = 1):
    playlist = await playlist_service.create_playlist(
        user_id=user_id, name=body.name, description=body.description
    )
    return PlaylistResponse(**playlist, track_count=0)


@router.get("", response_model=list[PlaylistResponse])
async def list_playlists(user_id: int = 1):
    playlists = await playlist_service.get_user_playlists(user_id)
    result = []
    for p in playlists:
        tracks = await PlaylistRepository.get_tracks(p["id"])
        thumbnail = tracks[0].get("thumbnail_url") if tracks else None
        result.append(
            PlaylistResponse(**p, track_count=len(tracks), thumbnail_url=thumbnail)
        )
    return result


@router.get("/{playlist_id}", response_model=PlaylistDetailResponse)
async def get_playlist(playlist_id: int):
    try:
        detail = await playlist_service.get_playlist_detail(playlist_id)
        detail.pop("tracks", None)
        tracks = await PlaylistRepository.get_tracks(playlist_id)
        track_responses = [TrackResponse(**t) for t in tracks]
        return PlaylistDetailResponse(**detail, tracks=track_responses)
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.put("/{playlist_id}", response_model=PlaylistResponse)
async def update_playlist(playlist_id: int, body: PlaylistUpdate):
    try:
        updates = {k: v for k, v in body.model_dump().items() if v is not None}
        playlist = await playlist_service.update_playlist(playlist_id, **updates)
        tracks = await PlaylistRepository.get_tracks(playlist_id)
        return PlaylistResponse(**playlist, track_count=len(tracks))
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.delete("/{playlist_id}", status_code=204)
async def delete_playlist(playlist_id: int):
    try:
        await playlist_service.delete_playlist(playlist_id)
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.post("/{playlist_id}/tracks")
async def add_track(playlist_id: int, youtube_id: str):
    try:
        result = await playlist_service.add_track_to_playlist(playlist_id, youtube_id)
        return {"status": "added", "playlist_id": playlist_id, "track_id": result["track_id"]}
    except (NotFoundException, ConflictException) as e:
        raise HTTPException(status_code=404 if isinstance(e, NotFoundException) else 409, detail=str(e))


@router.delete("/{playlist_id}/tracks")
async def remove_track(playlist_id: int, youtube_id: str):
    try:
        await playlist_service.remove_track_from_playlist(playlist_id, youtube_id)
        return {"status": "removed"}
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))


@router.put("/{playlist_id}/tracks/reorder")
async def reorder_tracks(playlist_id: int, track_ids: list[int]):
    try:
        await playlist_service.reorder_tracks(playlist_id, track_ids)
        return {"status": "reordered"}
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))


# ──────────────────────────────────────────────────────────────────────────────
# Import / Export
# ──────────────────────────────────────────────────────────────────────────────

@router.post("/import/text", response_model=PlaylistResponse, status_code=201)
async def import_from_text(body: ImportFromTextRequest):
    """
    Tạo playlist từ danh sách văn bản.
    Mỗi dòng là một bài hát ('Nghệ sĩ - Tên bài' hoặc chỉ 'Tên bài').
    Backend tìm kiếm yt-dlp cho từng dòng và thêm kết quả đầu tiên.
    Giới hạn 50 bài để tránh timeout.
    """
    lines = [l.strip() for l in body.tracks_text.strip().splitlines() if l.strip()][:50]
    playlist = await playlist_service.create_playlist(
        user_id=body.user_id, name=body.name, description=""
    )
    playlist_id = playlist["id"]
    added = 0
    for line in lines:
        try:
            results = await youtube_extractor.search(line, limit=1)
            if results:
                track = results[0]
                await playlist_service.add_track_to_playlist(playlist_id, track["youtube_id"])
                added += 1
        except Exception as e:
            logger.warning("Import skip '%s': %s", line, e)

    tracks = await PlaylistRepository.get_tracks(playlist_id)
    thumbnail = tracks[0].get("thumbnail_url") if tracks else None
    logger.info("Imported playlist '%s' with %d/%d tracks", body.name, added, len(lines))
    return PlaylistResponse(**playlist, track_count=len(tracks), thumbnail_url=thumbnail)


@router.get("/{playlist_id}/export")
async def export_playlist(playlist_id: int, format: str = Query("text")):
    """
    Xuất playlist ra văn bản thuần ('Nghệ sĩ - Tên bài', mỗi dòng một bài).
    """
    try:
        tracks = await PlaylistRepository.get_tracks(playlist_id)
    except NotFoundException as e:
        raise HTTPException(status_code=404, detail=str(e))

    if format == "text":
        lines = [
            f"{t.get('artist', '')} - {t.get('title', '')}".strip(" -")
            for t in tracks
        ]
        return PlainTextResponse("\n".join(lines))

    return {"tracks": [TrackResponse(**t) for t in tracks]}


# ──────────────────────────────────────────────────────────────────────────────
# YouTube sync endpoints
# ──────────────────────────────────────────────────────────────────────────────

@router.post("/youtube/sync-all")
async def sync_all_youtube_playlists(
    access_token: str = Query(..., description="YouTube OAuth access token"),
    user_id: int = Query(1),
):
    """
    Sync all playlists from the authenticated user's YouTube account.
    The client obtains the access_token via Google OAuth and passes it here.
    """
    try:
        stats = await youtube_sync_service.sync_all_playlists(user_id, access_token)
        return {"status": "success", **stats}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"YouTube sync failed: {str(e)}")


@router.post("/youtube/sync/{youtube_playlist_id}")
async def sync_single_youtube_playlist(
    youtube_playlist_id: str,
    access_token: str = Query(..., description="YouTube OAuth access token"),
    user_id: int = Query(1),
):
    """
    Sync a single YouTube playlist by its ID.
    """
    try:
        stats = await youtube_sync_service.sync_single_playlist(
            user_id, access_token, youtube_playlist_id
        )
        return {"status": "success", **stats}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Playlist sync failed: {str(e)}")
