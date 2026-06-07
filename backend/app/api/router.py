from fastapi import APIRouter

from app.api.v1 import playlists
from app.api.v1 import search
from app.api.v1 import auth
from app.api.v1 import player
from app.api.v1 import downloads
from app.api.v1 import favorites
from app.api.v1 import youtube_sync
from app.api.v1 import health
from app.api.v1 import history
from app.api.v1 import recommendations
from app.api.v1 import lyrics

api_router = APIRouter(prefix="/api/v1")

api_router.include_router(health.router, tags=["Health"])
api_router.include_router(history.router, prefix="/history", tags=["History"])
api_router.include_router(recommendations.router, prefix="/recommendations", tags=["Recommendations"])
api_router.include_router(auth.router, prefix="/auth", tags=["Auth"])
api_router.include_router(search.router, prefix="/search", tags=["Search"])
api_router.include_router(player.router, prefix="/player", tags=["Player"])
api_router.include_router(playlists.router, prefix="/playlists", tags=["Playlists"])
api_router.include_router(favorites.router, prefix="/favorites", tags=["Favorites"])
api_router.include_router(youtube_sync.router, prefix="/youtube-sync", tags=["YouTube Sync"])
api_router.include_router(downloads.router, prefix="/downloads", tags=["Downloads"])
api_router.include_router(lyrics.router, prefix="/lyrics", tags=["Lyrics"])
