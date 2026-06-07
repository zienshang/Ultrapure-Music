from app.db.database import fetch_one, fetch_all, execute, commit
from app.db.repositories.base import BaseRepository


class FavoriteRepository(BaseRepository):
    table_name = "favorites"

    @classmethod
    async def add(cls, user_id: int, track_id: int) -> dict | None:
        existing = await fetch_one(
            "SELECT * FROM favorites WHERE user_id = ? AND track_id = ?",
            (user_id, track_id),
        )
        if existing:
            return existing
        cursor = await execute(
            "INSERT INTO favorites (user_id, track_id) VALUES (?, ?)",
            (user_id, track_id),
        )
        await commit()
        return await cls.get_by_id(cursor.lastrowid)

    @classmethod
    async def remove(cls, user_id: int, track_id: int) -> bool:
        cursor = await execute(
            "DELETE FROM favorites WHERE user_id = ? AND track_id = ?",
            (user_id, track_id),
        )
        await commit()
        return cursor.rowcount > 0

    @classmethod
    async def get_by_user(cls, user_id: int) -> list[dict]:
        return await fetch_all(
            """SELECT t.*, f.created_at as favorited_at
               FROM tracks t
               JOIN favorites f ON t.id = f.track_id
               WHERE f.user_id = ?
               ORDER BY f.created_at DESC""",
            (user_id,),
        )

    @classmethod
    async def is_favorited(cls, user_id: int, track_id: int) -> bool:
        row = await fetch_one(
            "SELECT 1 as found FROM favorites WHERE user_id = ? AND track_id = ?",
            (user_id, track_id),
        )
        return row is not None

    @classmethod
    async def get_favorite_track_ids(cls, user_id: int) -> list[int]:
        rows = await fetch_all(
            "SELECT track_id FROM favorites WHERE user_id = ?",
            (user_id,),
        )
        return [r["track_id"] for r in rows]
