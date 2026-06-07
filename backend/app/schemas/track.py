from pydantic import BaseModel
from typing import Optional


class TrackResponse(BaseModel):
    id: Optional[int] = None
    youtube_id: str
    title: str
    artist: str = ""
    duration: int = 0
    thumbnail_url: Optional[str] = ""
    webpage_url: Optional[str] = ""
    uploaded_at: Optional[str] = ""
    view_count: int = 0


class TrackSearchResponse(BaseModel):
    query: str
    results: list[TrackResponse]
    total: int
    page: int
    page_size: int


class TrackDetailResponse(TrackResponse):
    is_favorited: bool = False
    stream_url: Optional[str] = None
