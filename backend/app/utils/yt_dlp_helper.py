import yt_dlp
from yt_dlp.utils import DownloadError, ExtractorError


class YouTubeDLHelper:
    def __init__(self):
        self._base_opts = {
            "quiet": True,
            "no_warnings": True,
            "extract_flat": False,
            "skip_download": True,
            "no_color": True,
        }

    def _build_opts(self, **overrides) -> dict:
        opts = {**self._base_opts, **overrides}
        return opts

    def extract_info(self, url: str, **extra_opts) -> dict:
        opts = self._build_opts(**extra_opts)
        with yt_dlp.YoutubeDL(opts) as ydl:
            return ydl.extract_info(url, download=False)

    def search(self, query: str, limit: int = 10) -> list[dict]:
        """
        Search YouTube and return basic track info.
        Uses extract_flat=True + ignoreerrors=True so:
        - Much faster (no per-video HTTP request)
        - Skips unavailable/private videos instead of crashing
        """
        search_url = f"ytsearch{limit}:{query}"
        opts = self._build_opts(
            extract_flat=True,
            ignoreerrors=True,
        )
        with yt_dlp.YoutubeDL(opts) as ydl:
            data = ydl.extract_info(search_url, download=False) or {}
        entries = data.get("entries", []) or []
        # Filter out None entries (unavailable/deleted videos)
        return [e for e in entries if e is not None and e.get("id")]

    def get_metadata(self, video_id: str) -> dict | None:
        url = f"https://www.youtube.com/watch?v={video_id}"
        return self.extract_info(url)

    def get_stream_url(self, video_id: str, quality: str = "bestaudio") -> str | None:
        url = f"https://www.youtube.com/watch?v={video_id}"
        opts = self._build_opts(format=quality)
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            return info.get("url")

    def get_audio_formats(self, video_id: str) -> list[dict]:
        url = f"https://www.youtube.com/watch?v={video_id}"
        opts = self._build_opts(format="bestaudio")
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False)
            return info.get("requested_formats", info.get("formats", []))

    def get_thumbnail_url(self, video_id: str) -> str | None:
        try:
            info = self.get_metadata(video_id)
            if info:
                return info.get("thumbnail")
        except (DownloadError, ExtractorError):
            pass
        return f"https://i.ytimg.com/vi/{video_id}/hqdefault.jpg"


ytdl_helper = YouTubeDLHelper()
