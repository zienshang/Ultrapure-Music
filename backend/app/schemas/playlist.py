from pydantic import BaseModel
from typing import Optional

from app.schemas.track import TrackResponse


class PlaylistCreate(BaseModel):
    name: str
    description: str = ""


class PlaylistUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None


class PlaylistResponse(BaseModel):
    id: int
    user_id: int
    name: str
    description: str = ""
    youtube_playlist_id: Optional[str] = None
    is_synced: bool = False
    track_count: int = 0
    thumbnail_url: Optional[str] = None
    created_at: str = ""
    updated_at: str = ""


class PlaylistDetailResponse(PlaylistResponse):
    tracks: list[TrackResponse] = []


class YouTubePlaylistResponse(BaseModel):
    """A public YouTube playlist surfaced from search.list (type=playlist)."""
    youtube_playlist_id: str
    name: str
    channel: str = ""
    thumbnail_url: Optional[str] = None
    track_count: int = 0
