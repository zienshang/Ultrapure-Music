import pytest


@pytest.mark.asyncio
class TestHealthAPI:
    async def test_root(self, client):
        resp = await client.get("/")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "running"
        assert data["app"] == "UltrapureMusic"

    async def test_health(self, client):
        resp = await client.get("/api/v1/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "healthy"
        assert data["database"] == "connected"


@pytest.mark.asyncio
class TestSearchAPI:
    async def test_search_requires_query(self, client):
        resp = await client.get("/api/v1/search")
        assert resp.status_code == 422

    async def test_search_with_query(self, client):
        resp = await client.get("/api/v1/search?q=test&page=1&page_size=5")
        assert resp.status_code == 200
        data = resp.json()
        assert data["query"] == "test"
        assert "results" in data
        assert data["page"] == 1
        assert data["page_size"] == 5


@pytest.mark.asyncio
class TestPlaylistAPI:
    async def _ensure_user(self):
        from app.db.repositories.users import UserRepository
        user = await UserRepository.find_or_create(
            email="playlist_tester@test.com",
            display_name="Playlist Tester",
        )
        return user["id"]

    async def test_create_playlist(self, client):
        uid = await self._ensure_user()
        resp = await client.post(
            "/api/v1/playlists",
            json={"name": "My Playlist", "description": "Test"},
            params={"user_id": uid},
        )
        assert resp.status_code == 201
        data = resp.json()
        assert data["name"] == "My Playlist"

    async def test_list_playlists(self, client):
        resp = await client.get("/api/v1/playlists")
        assert resp.status_code == 200
        assert isinstance(resp.json(), list)

    async def test_create_and_get_detail(self, client, sample_track):
        uid = await self._ensure_user()
        pl_resp = await client.post(
            "/api/v1/playlists",
            json={"name": "Detail Test"},
            params={"user_id": uid},
        )
        pl_id = pl_resp.json()["id"]

        await client.post(
            f"/api/v1/playlists/{pl_id}/tracks?youtube_id={sample_track['youtube_id']}",
        )

        detail = await client.get(f"/api/v1/playlists/{pl_id}")
        assert detail.status_code == 200
        assert detail.json()["track_count"] >= 1

    async def test_delete_playlist(self, client):
        uid = await self._ensure_user()
        pl = await client.post(
            "/api/v1/playlists",
            json={"name": "To Delete"},
            params={"user_id": uid},
        )
        pl_id = pl.json()["id"]
        resp = await client.delete(f"/api/v1/playlists/{pl_id}")
        assert resp.status_code == 204


@pytest.mark.asyncio
class TestFavoritesAPI:
    async def test_toggle_and_status(self, client, sample_track):
        from app.db.repositories.users import UserRepository
        user = await UserRepository.find_or_create(
            email="fav_tester@test.com",
            display_name="Fav Tester",
        )
        uid = user["id"]
        yt_id = sample_track["youtube_id"]
        toggle = await client.post(f"/api/v1/favorites?youtube_id={yt_id}&user_id={uid}")
        assert toggle.status_code == 200
        assert toggle.json()["favorited"] is True

        status = await client.get(f"/api/v1/favorites/status?youtube_id={yt_id}&user_id={uid}")
        assert status.json()["is_favorited"] is True

        toggle2 = await client.post(f"/api/v1/favorites?youtube_id={yt_id}&user_id={uid}")
        assert toggle2.json()["favorited"] is False


@pytest.mark.asyncio
class TestHistoryAPI:
    async def test_record_and_get(self, client, sample_track):
        from app.db.repositories.users import UserRepository
        user = await UserRepository.find_or_create(
            email="hist_tester@test.com",
            display_name="Hist Tester",
        )
        uid = user["id"]
        record = await client.post(
            f"/api/v1/history?youtube_id={sample_track['youtube_id']}&user_id={uid}&duration_played=30",
        )
        assert record.status_code == 200
        assert record.json()["status"] == "recorded"

        history = await client.get(f"/api/v1/history?user_id={uid}")
        assert history.status_code == 200
        data = history.json()
        assert data["total"] >= 1
        assert len(data["entries"]) >= 1

    async def test_recent_and_most_played(self, client, sample_track):
        from app.db.repositories.users import UserRepository
        user = await UserRepository.find_or_create(
            email="hist2_tester@test.com",
            display_name="Hist2 Tester",
        )
        uid = user["id"]
        await client.post(
            f"/api/v1/history?youtube_id={sample_track['youtube_id']}&user_id={uid}",
        )
        recent = await client.get(f"/api/v1/history/recent?user_id={uid}")
        assert recent.status_code == 200
        assert len(recent.json()) >= 1

        most = await client.get(f"/api/v1/history/most-played?user_id={uid}")
        assert most.status_code == 200
