from fastapi import APIRouter, Query, HTTPException

from app.services.youtube_sync_service import youtube_sync_service

router = APIRouter()


@router.post("/sync-all")
async def sync_all_playlists(access_token: str = Query(...), user_id: int = 1):
    try:
        stats = await youtube_sync_service.sync_all_playlists(user_id, access_token)
        return {"status": "completed", **stats}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Sync failed: {str(e)}")


@router.post("/sync-playlist")
async def sync_playlist(
    youtube_playlist_id: str = Query(...),
    access_token: str = Query(...),
    user_id: int = 1,
):
    try:
        stats = await youtube_sync_service.sync_single_playlist(
            user_id, access_token, youtube_playlist_id
        )
        return {"status": "completed", **stats}
    except Exception as e:
        raise HTTPException(status_code=400, detail=f"Sync failed: {str(e)}")
