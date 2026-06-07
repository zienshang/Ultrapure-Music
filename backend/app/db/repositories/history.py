from app.db.database import fetch_one, fetch_all, execute, commit
from app.db.repositories.base import BaseRepository


class HistoryRepository(BaseRepository):
    table_name = "history"

    @classmethod
    async def add(cls, user_id: int, track_id: int, duration_played: int = 0) -> dict:
        cursor = await execute(
            "INSERT INTO history (user_id, track_id, duration_played) VALUES (?, ?, ?)",
            (user_id, track_id, duration_played),
        )
        await commit()
        return await cls.get_by_id(cursor.lastrowid)

    @classmethod
    async def get_by_user(cls, user_id: int, limit: int = 50, offset: int = 0) -> list[dict]:
        return await fetch_all(
            """SELECT t.*, h.played_at, h.duration_played
               FROM tracks t
               JOIN history h ON t.id = h.track_id
               WHERE h.user_id = ?
               ORDER BY h.played_at DESC
               LIMIT ? OFFSET ?""",
            (user_id, limit, offset),
        )

    @classmethod
    async def get_most_played(cls, user_id: int, limit: int = 20) -> list[dict]:
        return await fetch_all(
            """SELECT t.*, COUNT(*) as play_count, SUM(h.duration_played) as total_duration
               FROM tracks t
               JOIN history h ON t.id = h.track_id
               WHERE h.user_id = ?
               GROUP BY t.id
               ORDER BY play_count DESC
               LIMIT ?""",
            (user_id, limit),
        )

    @classmethod
    async def get_global_most_played(cls, limit: int = 20) -> list[dict]:
        """Most-played tracks across ALL users (community popularity)."""
        return await fetch_all(
            """SELECT t.*, COUNT(*) as play_count
               FROM tracks t
               JOIN history h ON t.id = h.track_id
               GROUP BY t.id
               ORDER BY play_count DESC
               LIMIT ?""",
            (limit,),
        )

    @classmethod
    async def get_recent_tracks(cls, user_id: int, limit: int = 10) -> list[dict]:
        return await fetch_all(
            """SELECT DISTINCT t.*, h.played_at
               FROM tracks t
               JOIN history h ON t.id = h.track_id
               WHERE h.user_id = ?
               ORDER BY h.played_at DESC
               LIMIT ?""",
            (user_id, limit),
        )

    @classmethod
    async def clear_for_user(cls, user_id: int) -> bool:
        cursor = await execute("DELETE FROM history WHERE user_id = ?", (user_id,))
        await commit()
        return cursor.rowcount > 0

    @classmethod
    async def count_for_user(cls, user_id: int) -> int:
        row = await fetch_one(
            "SELECT COUNT(*) as cnt FROM history WHERE user_id = ?",
            (user_id,),
        )
        return row["cnt"] if row else 0
