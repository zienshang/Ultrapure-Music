import re


def sanitize_string(value: str | None, max_length: int = 500) -> str:
    if not value:
        return ""
    cleaned = value.strip()
    cleaned = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", "", cleaned)
    return cleaned[:max_length]


def validate_youtube_id(video_id: str) -> bool:
    if not video_id or len(video_id) > 20:
        return False
    return bool(re.match(r"^[a-zA-Z0-9_-]{11}$", video_id))


def validate_email(email: str) -> bool:
    pattern = r"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"
    return bool(re.match(pattern, email))
