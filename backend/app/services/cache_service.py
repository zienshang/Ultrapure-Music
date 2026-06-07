import json
import time
from pathlib import Path

import httpx

from app.config.constants import CACHE_DIR, TRACK_CACHE_TTL, THUMBNAIL_DIR
from app.utils.logger import get_logger

logger = get_logger(__name__)

_metadata_cache: dict[str, dict] = {}


class CacheService:

    @staticmethod
    async def get_track_metadata(youtube_id: str) -> dict | None:
        cached = _metadata_cache.get(youtube_id)
        if cached and time.time() - cached["_cached_at"] < TRACK_CACHE_TTL:
            return cached

        file_data = CacheService._read_file(youtube_id)
        if file_data and time.time() - file_data["_cached_at"] < TRACK_CACHE_TTL:
            _metadata_cache[youtube_id] = file_data
            return file_data
        return None

    @staticmethod
    async def set_track_metadata(youtube_id: str, data: dict) -> None:
        data["_cached_at"] = time.time()
        _metadata_cache[youtube_id] = data
        CacheService._write_file(youtube_id, data)

    @staticmethod
    async def download_thumbnail(url: str, video_id: str) -> str | None:
        thumb_dir = Path(THUMBNAIL_DIR)
        thumb_dir.mkdir(parents=True, exist_ok=True)
        thumb_path = thumb_dir / f"{video_id}.jpg"
        if thumb_path.exists():
            return str(thumb_path)

        try:
            async with httpx.AsyncClient(timeout=15) as client:
                resp = await client.get(url)
                if resp.status_code == 200:
                    thumb_path.write_bytes(resp.content)
                    return str(thumb_path)
        except Exception as e:
            logger.warning("Thumbnail download failed for %s: %s", video_id, e)
        return None

    @staticmethod
    async def get_thumbnail_path(video_id: str) -> Path | None:
        thumb_path = Path(THUMBNAIL_DIR) / f"{video_id}.jpg"
        return thumb_path if thumb_path.exists() else None

    @staticmethod
    def _cache_path(youtube_id: str) -> Path:
        return Path(CACHE_DIR) / "metadata" / f"{youtube_id}.json"

    @staticmethod
    def _read_file(youtube_id: str) -> dict | None:
        path = CacheService._cache_path(youtube_id)
        try:
            if path.exists():
                return json.loads(path.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, OSError) as e:
            logger.warning("Failed to read metadata cache: %s", e)
        return None

    @staticmethod
    def _write_file(youtube_id: str, data: dict) -> None:
        path = CacheService._cache_path(youtube_id)
        try:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, default=str), encoding="utf-8")
        except OSError as e:
            logger.warning("Failed to write metadata cache: %s", e)

    @staticmethod
    async def clear_expired_metadata() -> int:
        now = time.time()
        count = 0
        for youtube_id in list(_metadata_cache.keys()):
            if now - _metadata_cache[youtube_id].get("_cached_at", 0) >= TRACK_CACHE_TTL:
                del _metadata_cache[youtube_id]
                count += 1

        cache_dir = Path(CACHE_DIR) / "metadata"
        if cache_dir.exists():
            for f in cache_dir.iterdir():
                if f.suffix == ".json":
                    try:
                        data = json.loads(f.read_text(encoding="utf-8"))
                        if now - data.get("_cached_at", 0) >= TRACK_CACHE_TTL:
                            f.unlink()
                            count += 1
                    except (json.JSONDecodeError, OSError):
                        pass
        return count

    @staticmethod
    async def clear_all() -> None:
        _metadata_cache.clear()
        for d in [Path(CACHE_DIR) / "metadata"]:
            if d.exists():
                for f in d.iterdir():
                    f.unlink()


cache_service = CacheService()
