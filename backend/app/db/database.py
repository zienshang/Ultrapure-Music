import aiosqlite
from pathlib import Path

from app.config.settings import settings

_db: aiosqlite.Connection | None = None


def _get_db_path() -> str:
    db_url = settings.database_url
    if db_url.startswith("sqlite:///"):
        return db_url[len("sqlite:///"):]
    return db_url


async def get_connection() -> aiosqlite.Connection:
    global _db
    if _db is None:
        db_path = _get_db_path()
        Path(db_path).parent.mkdir(parents=True, exist_ok=True)
        _db = await aiosqlite.connect(db_path)
        _db.row_factory = aiosqlite.Row
        await _db.execute("PRAGMA journal_mode=WAL")
        await _db.execute("PRAGMA foreign_keys=ON")
    return _db


async def close_connection() -> None:
    global _db
    if _db is not None:
        await _db.close()
        _db = None


async def execute(sql: str, params: tuple = ()) -> aiosqlite.Cursor:
    db = await get_connection()
    return await db.execute(sql, params)


async def execute_many(sql: str, params_list: list[tuple]) -> None:
    db = await get_connection()
    await db.executemany(sql, params_list)


async def fetch_one(sql: str, params: tuple = ()) -> dict | None:
    db = await get_connection()
    cursor = await db.execute(sql, params)
    row = await cursor.fetchone()
    return dict(row) if row else None


async def fetch_all(sql: str, params: tuple = ()) -> list[dict]:
    db = await get_connection()
    cursor = await db.execute(sql, params)
    rows = await cursor.fetchall()
    return [dict(row) for row in rows]


async def commit() -> None:
    db = await get_connection()
    await db.commit()


async def rollback() -> None:
    db = await get_connection()
    await db.rollback()
