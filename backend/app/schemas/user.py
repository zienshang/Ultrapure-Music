from pydantic import BaseModel
from typing import Optional


class UserCreate(BaseModel):
    email: str
    display_name: str
    google_id: Optional[str] = None
    avatar_url: Optional[str] = None


class UserUpdate(BaseModel):
    display_name: Optional[str] = None
    avatar_url: Optional[str] = None
