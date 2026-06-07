from app.db.database import fetch_one, fetch_all, execute, commit
from app.db.repositories.base import BaseRepository


class TrackRepository(BaseRepository):
    table_name = "tracks"

    @classmethod
    async def create(cls, youtube_id: str, title: str, artist: str = "", duration: int = 0,
                     thumbnail_url: str | None = None, webpage_url: str | None = None,
                     uploaded_at: str | None = None, view_count: int = 0,
                     metadata_json: str | None = None) -> dict:
        cursor = await execute(
            """INSERT INTO tracks (youtube_id, title, artist, duration, thumbnail_url,
               webpage_url, uploaded_at, view_count, metadata_json)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            (youtube_id, title, artist, duration, thumbnail_url, webpage_url,
             uploaded_at, view_count, metadata_json),
        )
        await commit()
        return await cls.get_by_id(cursor.lastrowid)

    @classmethod
    async def get_by_youtube_id(cls, youtube_id: str) -> dict | None:
        return await fetch_one("SELECT * FROM tracks WHERE youtube_id = ?", (youtube_id,))

    @classmethod
    async def find_or_create(cls, youtube_id: str, title: str, artist: str = "",
                             duration: int = 0, thumbnail_url: str | None = None,
                             webpage_url: str | None = None, uploaded_at: str | None = None,
                             view_count: int = 0, metadata_json: str | None = None) -> dict:
        existing = await cls.get_by_youtube_id(youtube_id)
        if existing:
            return existing
        return await cls.create(youtube_id, title, artist, duration, thumbnail_url,
                                webpage_url, uploaded_at, view_count, metadata_json)

    @classmethod
    async def search_by_title(cls, query: str, limit: int = 20) -> list[dict]:
        return await fetch_all(
            "SELECT * FROM tracks WHERE title LIKE ? ORDER BY view_count DESC LIMIT ?",
            (f"%{query}%", limit),
        )

    @classmethod
    async def update_metadata(cls, youtube_id: str, **kwargs) -> dict | None:
        allowed = {"title", "artist", "duration", "thumbnail_url", "view_count", "metadata_json"}
        updates = {k: v for k, v in kwargs.items() if k in allowed and v is not None}
        if not updates:
            return await cls.get_by_youtube_id(youtube_id)
        set_clause = ", ".join(f"{k} = ?" for k in updates)
        values = list(updates.values()) + [youtube_id]
        await execute(
            f"UPDATE tracks SET {set_clause}, updated_at = datetime('now') WHERE youtube_id = ?",
            values,
        )
        await commit()
        return await cls.get_by_youtube_id(youtube_id)
