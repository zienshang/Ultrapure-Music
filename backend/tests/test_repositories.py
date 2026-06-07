import pytest

from app.db.repositories.users import UserRepository
from app.db.repositories.tracks import TrackRepository
from app.db.repositories.playlists import PlaylistRepository
from app.db.repositories.favorites import FavoriteRepository
from app.db.repositories.history import HistoryRepository


@pytest.mark.asyncio
class TestUserRepository:
    async def test_create_and_get(self, setup_db):
        user = await UserRepository.create(
            email="alice@example.com",
            display_name="Alice",
            google_id="g123",
        )
        assert user["email"] == "alice@example.com"
        assert user["display_name"] == "Alice"

        fetched = await UserRepository.get_by_id(user["id"])
        assert fetched["email"] == "alice@example.com"

    async def test_find_or_create_existing(self):
        user1 = await UserRepository.find_or_create(
            email="existing@example.com",
            display_name="Existing",
        )
        user2 = await UserRepository.find_or_create(
            email="existing@example.com",
            display_name="Existing",
        )
        assert user1["id"] == user2["id"]

    async def test_get_by_google_id(self, setup_db):
        user = await UserRepository.create(
            email="google@example.com",
            display_name="Google User",
            google_id="google456",
        )
        fetched = await UserRepository.get_by_google_id("google456")
        assert fetched["id"] == user["id"]


@pytest.mark.asyncio
class TestTrackRepository:
    async def test_create_and_get(self, setup_db):
        track = await TrackRepository.create(
            youtube_id="test123",
            title="Test Track",
            duration=180,
        )
        assert track["youtube_id"] == "test123"

        fetched = await TrackRepository.get_by_youtube_id("test123")
        assert fetched["title"] == "Test Track"

    async def test_find_or_create(self):
        t1 = await TrackRepository.find_or_create(
            youtube_id="dup123",
            title="Duplicate",
        )
        t2 = await TrackRepository.find_or_create(
            youtube_id="dup123",
            title="Duplicate",
        )
        assert t1["id"] == t2["id"]

    async def test_search_by_title(self, setup_db):
        await TrackRepository.create(youtube_id="s1", title="Hello World")
        await TrackRepository.create(youtube_id="s2", title="Hello Again")
        results = await TrackRepository.search_by_title("Hello")
        assert len(results) >= 2


@pytest.mark.asyncio
class TestPlaylistRepository:
    async def test_create_and_add_track(self, sample_user, sample_track):
        pl = await PlaylistRepository.create(
            user_id=sample_user["id"],
            name="Test Playlist",
        )
        assert pl["name"] == "Test Playlist"

        pt = await PlaylistRepository.add_track(pl["id"], sample_track["id"])
        assert pt["playlist_id"] == pl["id"]
        assert pt["track_id"] == sample_track["id"]

        tracks = await PlaylistRepository.get_tracks(pl["id"])
        assert len(tracks) == 1

    async def test_remove_track(self, sample_user, sample_track):
        pl = await PlaylistRepository.create(
            user_id=sample_user["id"],
            name="Remove Test",
        )
        await PlaylistRepository.add_track(pl["id"], sample_track["id"])
        deleted = await PlaylistRepository.remove_track(pl["id"], sample_track["id"])
        assert deleted is True
        tracks = await PlaylistRepository.get_tracks(pl["id"])
        assert len(tracks) == 0


@pytest.mark.asyncio
class TestFavoriteRepository:
    async def test_add_and_check(self, sample_user, sample_track):
        fav = await FavoriteRepository.add(sample_user["id"], sample_track["id"])
        assert fav is not None

        is_fav = await FavoriteRepository.is_favorited(sample_user["id"], sample_track["id"])
        assert is_fav is True

    async def test_remove(self, sample_user, sample_track):
        await FavoriteRepository.add(sample_user["id"], sample_track["id"])
        deleted = await FavoriteRepository.remove(sample_user["id"], sample_track["id"])
        assert deleted is True

        is_fav = await FavoriteRepository.is_favorited(sample_user["id"], sample_track["id"])
        assert is_fav is False


@pytest.mark.asyncio
class TestHistoryRepository:
    async def test_add_and_get(self, sample_user, sample_track):
        entry = await HistoryRepository.add(
            user_id=sample_user["id"],
            track_id=sample_track["id"],
            duration_played=30,
        )
        assert entry["user_id"] == sample_user["id"]

        recent = await HistoryRepository.get_recent_tracks(sample_user["id"])
        assert len(recent) >= 1

    async def test_most_played(self, sample_user, sample_track):
        await HistoryRepository.add(sample_user["id"], sample_track["id"], 30)
        await HistoryRepository.add(sample_user["id"], sample_track["id"], 60)
        most = await HistoryRepository.get_most_played(sample_user["id"])
        assert len(most) >= 1
        assert most[0]["play_count"] >= 2
