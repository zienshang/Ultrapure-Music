import time
import jwt

from app.utils.token_utils import (
    create_access_token,
    verify_access_token,
    create_stream_token,
    verify_stream_token,
)
from app.utils.validation import (
    sanitize_string,
    validate_youtube_id,
    validate_email,
)
from app.utils.rate_limiter import RateLimiter
from app.config.settings import settings


class TestTokenUtils:
    def test_create_and_verify_access_token(self):
        token = create_access_token(user_id=1, email="test@test.com")
        payload = verify_access_token(token)
        assert payload is not None
        assert payload["user_id"] == 1
        assert payload["email"] == "test@test.com"
        assert payload["type"] == "access"

    def test_create_and_verify_stream_token(self):
        token = create_stream_token(video_id="dQw4w9WgXcQ", user_id=1)
        payload = verify_stream_token(token)
        assert payload is not None
        assert payload["video_id"] == "dQw4w9WgXcQ"
        assert payload["user_id"] == 1
        assert payload["type"] == "stream"

    def test_expired_token(self):
        payload = {
            "type": "access",
            "user_id": 1,
            "email": "test@test.com",
            "exp": time.time() - 10,
            "iat": time.time() - 60,
        }
        token = jwt.encode(payload, settings.secret_key, algorithm=settings.jwt_algorithm)
        assert verify_access_token(token) is None


class TestValidation:
    def test_sanitize_string(self):
        assert sanitize_string("  hello  ") == "hello"
        assert sanitize_string(None) == ""
        assert sanitize_string("a" * 1000) == "a" * 500

    def test_validate_youtube_id(self):
        assert validate_youtube_id("dQw4w9WgXcQ") is True
        assert validate_youtube_id("invalid") is False
        assert validate_youtube_id("") is False
        assert validate_youtube_id("a" * 30) is False

    def test_validate_email(self):
        assert validate_email("test@example.com") is True
        assert validate_email("not-an-email") is False
        assert validate_email("") is False


class TestRateLimiter:
    def test_rate_limiting(self):
        limiter = RateLimiter(max_requests=3, window_seconds=60)
        assert limiter.is_allowed("key1") is True
        assert limiter.is_allowed("key1") is True
        assert limiter.is_allowed("key1") is True
        assert limiter.is_allowed("key1") is False

    def test_rate_limiter_different_keys(self):
        limiter = RateLimiter(max_requests=1, window_seconds=60)
        assert limiter.is_allowed("a") is True
        assert limiter.is_allowed("a") is False
        assert limiter.is_allowed("b") is True
