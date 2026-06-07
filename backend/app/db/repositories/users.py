from app.db.database import fetch_one, execute, commit
from app.db.repositories.base import BaseRepository


class UserRepository(BaseRepository):
    table_name = "users"

    @classmethod
    async def create(cls, email: str, display_name: str, google_id: str | None = None, avatar_url: str | None = None) -> dict:
        cursor = await execute(
            "INSERT INTO users (email, display_name, google_id, avatar_url) VALUES (?, ?, ?, ?)",
            (email, display_name, google_id, avatar_url),
        )
        await commit()
        return await cls.get_by_id(cursor.lastrowid)

    @classmethod
    async def get_by_email(cls, email: str) -> dict | None:
        return await fetch_one("SELECT * FROM users WHERE email = ?", (email,))

    @classmethod
    async def get_by_google_id(cls, google_id: str) -> dict | None:
        return await fetch_one("SELECT * FROM users WHERE google_id = ?", (google_id,))

    @classmethod
    async def update(cls, id: int, **kwargs) -> dict | None:
        allowed = {"email", "display_name", "google_id", "avatar_url"}
        updates = {k: v for k, v in kwargs.items() if k in allowed and v is not None}
        if not updates:
            return await cls.get_by_id(id)
        set_clause = ", ".join(f"{k} = ?" for k in updates)
        values = list(updates.values()) + [id]
        await execute(
            f"UPDATE users SET {set_clause}, updated_at = datetime('now') WHERE id = ?",
            values,
        )
        await commit()
        return await cls.get_by_id(id)

    @classmethod
    async def find_or_create(cls, email: str, display_name: str, google_id: str | None = None, avatar_url: str | None = None) -> dict:
        existing = await cls.get_by_email(email)
        if existing:
            return existing
        return await cls.create(email, display_name, google_id, avatar_url)
