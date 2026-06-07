import random
import time

from app.db.repositories.history import HistoryRepository
from app.db.repositories.favorites import FavoriteRepository
from app.services.youtube_extractor import youtube_extractor
from app.utils.logger import get_logger

logger = get_logger(__name__)

# Used when the user has no listening history / favorites yet.
_FALLBACK_QUERIES = ["nhạc trẻ hay nhất", "top hits 2024", "v-pop hot", "nhạc us uk"]

_cache: dict[int, dict] = {}
_CACHE_TTL = 1800  # 30 minutes


class RecommendationService:
    """
    Builds a 'For you' feed from the user's taste signals (most-played tracks and
    favorites). Extracts the user's favourite artists and finds more songs from
    them via yt-dlp, excluding tracks the user already knows. Falls back to
    generic popular queries for new users. Results are cached per user.
    """

    @staticmethod
    async def get_recommendations(user_id: int, limit: int = 20) -> list[dict]:
        cached = _cache.get(user_id)
        if cached and time.time() - cached["timestamp"] < _CACHE_TTL:
            logger.debug("Recommendation cache hit for user %d", user_id)
            return cached["data"]

        most_played = await HistoryRepository.get_most_played(user_id, limit=10)
        favorites = await FavoriteRepository.get_by_user(user_id)

        known_ids: set[str] = set()
        for t in (most_played + favorites):
            if t.get("youtube_id"):
                known_ids.add(t["youtube_id"])

        # Favourite artists, most-played first.
        artists: list[str] = []
        for t in (most_played + favorites):
            artist = (t.get("artist") or "").strip()
            if artist and artist not in artists:
                artists.append(artist)
            if len(artists) >= 3:
                break

        seeds = artists if artists else random.sample(_FALLBACK_QUERIES, k=2)
        personalized = bool(artists)

        results: list[dict] = []
        seen = set(known_ids)
        per_seed = max(5, (limit // max(1, len(seeds))) + 2)

        for seed in seeds:
            try:
                entries = await youtube_extractor.search(seed, limit=per_seed)
            except Exception as e:
                logger.warning("Recommendation search failed for '%s': %s", seed, e)
                continue
            for item in entries:
                vid = item.get("youtube_id")
                if not vid or vid in seen:
                    continue
                seen.add(vid)
                results.append(item)
            if len(results) >= limit:
                break

        random.shuffle(results)
        results = results[:limit]

        if results:
            _cache[user_id] = {"data": results, "timestamp": time.time()}

        logger.info(
            "Recommendations for user %d: %d tracks (personalized=%s, seeds=%s)",
            user_id, len(results), personalized, seeds,
        )
        return results

    @staticmethod
    def invalidate(user_id: int) -> None:
        _cache.pop(user_id, None)


recommendation_service = RecommendationService()
