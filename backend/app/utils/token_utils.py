import time

import jwt

from app.config.settings import settings


def create_stream_token(video_id: str, user_id: int | None = None) -> str:
    payload = {
        "type": "stream",
        "video_id": video_id,
        "user_id": user_id,
        "exp": time.time() + 3600,
        "iat": time.time(),
    }
    return jwt.encode(payload, settings.secret_key, algorithm=settings.jwt_algorithm)


def verify_stream_token(token: str) -> dict | None:
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=[settings.jwt_algorithm])
        if payload.get("type") != "stream":
            return None
        return payload
    except jwt.PyJWTError:
        return None


def create_access_token(user_id: int, email: str) -> str:
    payload = {
        "type": "access",
        "user_id": user_id,
        "email": email,
        "exp": time.time() + settings.jwt_expiration_minutes * 60,
        "iat": time.time(),
    }
    return jwt.encode(payload, settings.secret_key, algorithm=settings.jwt_algorithm)


def verify_access_token(token: str) -> dict | None:
    try:
        payload = jwt.decode(token, settings.secret_key, algorithms=[settings.jwt_algorithm])
        if payload.get("type") != "access":
            return None
        return payload
    except jwt.PyJWTError:
        return None
