import asyncio
from pathlib import Path

import pytest
from httpx import AsyncClient, ASGITransport

from app.db.database import close_connection
from app.db.migrations import run_migrations
from app.main import app


@pytest.fixture(scope="session")
def event_loop():
    loop = asyncio.new_event_loop()
    yield loop
    loop.close()


@pytest.fixture
async def setup_db():
    db_path = Path("data/test.db")
    db_path.parent.mkdir(parents=True, exist_ok=True)
    import app.db.database as db_mod
    import app.config.settings as settings_mod
    original_url = settings_mod.settings.database_url
    settings_mod.settings.database_url = f"sqlite:///./data/test.db"
    db_mod._db = None
    await run_migrations()
    yield
    await close_connection()
    settings_mod.settings.database_url = original_url
    if db_path.exists():
        db_path.unlink()


@pytest.fixture
async def client(setup_db):
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest.fixture
async def sample_user(setup_db):
    from app.db.repositories.users import UserRepository
    user = await UserRepository.create(
        email="test@example.com",
        display_name="Test User",
        google_id="google123",
    )
    return user


@pytest.fixture
async def sample_track(setup_db):
    from app.db.repositories.tracks import TrackRepository
    track = await TrackRepository.create(
        youtube_id="dQw4w9WgXcQ",
        title="Never Gonna Give You Up",
        artist="Rick Astley",
        duration=212,
        thumbnail_url="https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    )
    return track
