from pathlib import Path
from fastapi import APIRouter

from app.config.constants import CACHE_DIR, THUMBNAIL_DIR
from app.services.cache_service import cache_service
from app.services.stream_service import stream_service
from app.utils.logger import get_logger

router = APIRouter()
logger = get_logger(__name__)


@router.get("/cache/stats")
async def cache_stats():
    stream_cache_count = stream_service.cache_size()

    thumb_dir = Path(THUMBNAIL_DIR)
    thumb_count = len(list(thumb_dir.glob("*.jpg"))) if thumb_dir.exists() else 0

    meta_dir = Path(CACHE_DIR) / "metadata"
    meta_count = len(list(meta_dir.glob("*.json"))) if meta_dir.exists() else 0

    stream_dir = Path(CACHE_DIR) / "streams"
    stream_file_count = len(list(stream_dir.glob("*.json"))) if stream_dir.exists() else 0

    return {
        "in_memory_stream_cache": stream_cache_count,
        "file_metadata_cache": meta_count,
        "file_stream_cache": stream_file_count,
        "thumbnail_files": thumb_count,
        "cache_dirs": {
            "cache": str(CACHE_DIR),
            "thumbnails": str(THUMBNAIL_DIR),
        },
    }


@router.post("/cache/clear")
async def clear_cache():
    await cache_service.clear_all()
    await stream_service.clear_all()
    logger.info("All caches cleared manually")
    return {"status": "cleared"}
