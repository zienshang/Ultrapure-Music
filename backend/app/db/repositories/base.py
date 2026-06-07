from app.db.database import fetch_one, fetch_all, execute, commit


class BaseRepository:
    table_name: str = ""

    @classmethod
    async def get_by_id(cls, id: int) -> dict | None:
        return await fetch_one(f"SELECT * FROM {cls.table_name} WHERE id = ?", (id,))

    @classmethod
    async def get_all(cls, limit: int = 100, offset: int = 0) -> list[dict]:
        return await fetch_all(
            f"SELECT * FROM {cls.table_name} ORDER BY id DESC LIMIT ? OFFSET ?",
            (limit, offset),
        )

    @classmethod
    async def delete(cls, id: int) -> bool:
        cursor = await execute(f"DELETE FROM {cls.table_name} WHERE id = ?", (id,))
        await commit()
        return cursor.rowcount > 0

    @classmethod
    async def count(cls) -> int:
        row = await fetch_one(f"SELECT COUNT(*) as cnt FROM {cls.table_name}")
        return row["cnt"] if row else 0
