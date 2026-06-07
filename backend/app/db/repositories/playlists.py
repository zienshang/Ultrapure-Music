from app.db.database import fetch_one, fetch_all, execute, commit
from app.db.repositories.base import BaseRepository


class PlaylistRepository(BaseRepository):
    table_name = "playlists"

    @classmethod
    async def create(cls, user_id: int, name: str, description: str = "",
                     youtube_playlist_id: str | None = None) -> dict:
        cursor = await execute(
            "INSERT INTO playlists (user_id, name, description, youtube_playlist_id) VALUES (?, ?, ?, ?)",
            (user_id, name, description, youtube_playlist_id),
        )
        await commit()
        return await cls.get_by_id(cursor.lastrowid)

    @classmethod
    async def get_by_user(cls, user_id: int) -> list[dict]:
        return await fetch_all(
            "SELECT * FROM playlists WHERE user_id = ? ORDER BY updated_at DESC",
            (user_id,),
        )

    @classmethod
    async def update(cls, id: int, **kwargs) -> dict | None:
        allowed = {"name", "description", "youtube_playlist_id", "is_synced"}
        updates = {k: v for k, v in kwargs.items() if k in allowed and v is not None}
        if not updates:
            return await cls.get_by_id(id)
        set_clause = ", ".join(f"{k} = ?" for k in updates)
        values = list(updates.values()) + [id]
        await execute(
            f"UPDATE playlists SET {set_clause}, updated_at = datetime('now') WHERE id = ?",
            values,
        )
        await commit()
        return await cls.get_by_id(id)

    @classmethod
    async def add_track(cls, playlist_id: int, track_id: int, position: int | None = None) -> dict:
        if position is None:
            row = await fetch_one(
                "SELECT COALESCE(MAX(position), -1) + 1 as pos FROM playlist_tracks WHERE playlist_id = ?",
                (playlist_id,),
            )
            position = row["pos"] if row else 0
        cursor = await execute(
            "INSERT INTO playlist_tracks (playlist_id, track_id, position) VALUES (?, ?, ?)",
            (playlist_id, track_id, position),
        )
        await commit()
        return await fetch_one("SELECT * FROM playlist_tracks WHERE id = ?", (cursor.lastrowid,))

    @classmethod
    async def remove_track(cls, playlist_id: int, track_id: int) -> bool:
        cursor = await execute(
            "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?",
            (playlist_id, track_id),
        )
        await commit()
        return cursor.rowcount > 0

    @classmethod
    async def get_tracks(cls, playlist_id: int) -> list[dict]:
        return await fetch_all(
            """SELECT t.*, pt.position, pt.added_at as added_to_playlist_at
               FROM tracks t
               JOIN playlist_tracks pt ON t.id = pt.track_id
               WHERE pt.playlist_id = ?
               ORDER BY pt.position ASC""",
            (playlist_id,),
        )

    @classmethod
    async def reorder_tracks(cls, playlist_id: int, track_ids: list[int]) -> None:
        for idx, track_id in enumerate(track_ids):
            await execute(
                "UPDATE playlist_tracks SET position = ? WHERE playlist_id = ? AND track_id = ?",
                (idx, playlist_id, track_id),
            )
        await commit()

    @classmethod
    async def get_by_youtube_id(cls, youtube_playlist_id: str) -> dict | None:
        return await fetch_one(
            "SELECT * FROM playlists WHERE youtube_playlist_id = ?",
            (youtube_playlist_id,),
        )
