import time
import json
from pathlib import Path

from app.config.settings import settings
from app.config.constants import STREAM_CACHE_TTL, CACHE_DIR
from app.services.youtube_extractor import youtube_extractor
from app.utils.logger import get_logger

logger = get_logger(__name__)

_stream_cache: dict[str, dict] = {}


class StreamService:

    @staticmethod
    def _cache_key(video_id: str, quality: int) -> str:
        return video_id if quality == 0 else f"{video_id}_q{quality}"

    @staticmethod
    async def get_stream_url(video_id: str, quality: int = 0) -> str:
        key = StreamService._cache_key(video_id, quality)
        cached = StreamService._get_from_cache(key)
        if cached:
            logger.debug("Stream URL cache hit for %s (quality=%d)", video_id, quality)
            return cached["url"]

        url = await youtube_extractor.get_stream_url(video_id, quality=quality)
        StreamService._save_to_cache(key, url)
        return url

    @staticmethod
    async def refresh_stream_url(video_id: str, quality: int = 0) -> str:
        url = await youtube_extractor.get_stream_url(video_id, quality=quality)
        key = StreamService._cache_key(video_id, quality)
        StreamService._save_to_cache(key, url)
        logger.info("Stream URL refreshed for %s (quality=%d)", video_id, quality)
        return url

    @staticmethod
    def _get_from_cache(video_id: str) -> dict | None:
        cached = _stream_cache.get(video_id)
        if cached and time.time() - cached["timestamp"] < STREAM_CACHE_TTL:
            return cached

        file_cache = StreamService._read_file_cache(video_id)
        if file_cache and time.time() - file_cache["timestamp"] < STREAM_CACHE_TTL:
            _stream_cache[video_id] = file_cache
            return file_cache

        return None

    @staticmethod
    def _save_to_cache(video_id: str, url: str) -> None:
        entry = {"url": url, "timestamp": time.time()}
        _stream_cache[video_id] = entry
        StreamService._write_file_cache(video_id, entry)

    @staticmethod
    def _cache_path(video_id: str) -> Path:
        # video_id may already include quality suffix (e.g. "abc123_q2")
        safe = video_id.replace("/", "_")
        return Path(CACHE_DIR) / "streams" / f"{safe}.json"

    @staticmethod
    def _read_file_cache(video_id: str) -> dict | None:
        path = StreamService._cache_path(video_id)
        try:
            if path.exists():
                return json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as e:
            logger.warning("Failed to read stream cache for %s: %s", video_id, e)
        return None

    @staticmethod
    def _write_file_cache(video_id: str, entry: dict) -> None:
        path = StreamService._cache_path(video_id)
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(entry), encoding="utf-8")
        except OSError as e:
            logger.warning("Failed to write stream cache for %s: %s", video_id, e)

    @staticmethod
    async def clear_expired() -> int:
        now = time.time()
        expired = []
        for video_id, entry in list(_stream_cache.items()):
            if now - entry["timestamp"] >= STREAM_CACHE_TTL:
                expired.append(video_id)
                del _stream_cache[video_id]

        cache_dir = Path(CACHE_DIR) / "streams"
        if cache_dir.exists():
            for f in cache_dir.iterdir():
                if f.suffix == ".json":
                    try:
                        data = json.loads(f.read_text(encoding="utf-8"))
                        if now - data["timestamp"] >= STREAM_CACHE_TTL:
                            f.unlink()
                            expired.append(f.stem)
                    except (json.JSONDecodeError, OSError):
                        pass
        return len(expired)

    @staticmethod
    def cache_size() -> int:
        return len(_stream_cache)

    @staticmethod
    async def clear_all() -> None:
        _stream_cache.clear()
        cache_dir = Path(CACHE_DIR) / "streams"
        if cache_dir.exists():
            for f in cache_dir.iterdir():
                if f.suffix == ".json":
                    f.unlink()


stream_service = StreamService()
