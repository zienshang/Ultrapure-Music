import pytest
from app.utils.token_utils import create_access_token


@pytest.mark.asyncio
class TestAuthFlow:
    async def test_auth_url(self, client):
        resp = await client.get("/api/v1/auth/google/url")
        assert resp.status_code == 200
        data = resp.json()
        assert "auth_url" in data
        assert data["auth_url"].startswith("https://accounts.google.com/o/oauth2/auth")

    async def test_me_without_token(self, client):
        resp = await client.get("/api/v1/auth/me")
        assert resp.status_code == 401

    async def test_me_with_invalid_token(self, client):
        resp = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": "Bearer invalidtoken123"},
        )
        assert resp.status_code == 401

    async def test_me_with_valid_token(self, client, sample_user):
        token = create_access_token(
            user_id=sample_user["id"],
            email=sample_user["email"],
        )
        resp = await client.get(
            "/api/v1/auth/me",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["email"] == sample_user["email"]
        assert data["display_name"] == sample_user["display_name"]

    async def test_logout_with_valid_token(self, client, sample_user):
        token = create_access_token(
            user_id=sample_user["id"],
            email=sample_user["email"],
        )
        resp = await client.post(
            "/api/v1/auth/logout",
            headers={"Authorization": f"Bearer {token}"},
        )
        assert resp.status_code == 200
        assert resp.json()["status"] == "logged_out"

    async def test_cache_endpoints(self, client):
        stats = await client.get("/api/v1/downloads/cache/stats")
        assert stats.status_code == 200
        data = stats.json()
        assert "in_memory_stream_cache" in data
        assert "thumbnail_files" in data

        cleared = await client.post("/api/v1/downloads/cache/clear")
        assert cleared.status_code == 200
        assert cleared.json()["status"] == "cleared"
