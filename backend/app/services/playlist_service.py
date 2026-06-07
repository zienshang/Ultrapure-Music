from app.db.repositories.playlists import PlaylistRepository
from app.db.repositories.tracks import TrackRepository
from app.utils.exceptions import NotFoundException, ConflictException
from app.utils.logger import get_logger

logger = get_logger(__name__)


class PlaylistService:

    @staticmethod
    async def create_playlist(user_id: int, name: str, description: str = "") -> dict:
        return await PlaylistRepository.create(user_id, name, description)

    @staticmethod
    async def get_user_playlists(user_id: int) -> list[dict]:
        return await PlaylistRepository.get_by_user(user_id)

    @staticmethod
    async def get_playlist_detail(playlist_id: int) -> dict:
        playlist = await PlaylistRepository.get_by_id(playlist_id)
        if not playlist:
            raise NotFoundException("Playlist not found")
        tracks = await PlaylistRepository.get_tracks(playlist_id)
        playlist["tracks"] = tracks
        playlist["track_count"] = len(tracks)
        return playlist

    @staticmethod
    async def update_playlist(playlist_id: int, **kwargs) -> dict:
        playlist = await PlaylistRepository.get_by_id(playlist_id)
        if not playlist:
            raise NotFoundException("Playlist not found")
        return await PlaylistRepository.update(playlist_id, **kwargs)

    @staticmethod
    async def delete_playlist(playlist_id: int) -> bool:
        playlist = await PlaylistRepository.get_by_id(playlist_id)
        if not playlist:
            raise NotFoundException("Playlist not found")
        return await PlaylistRepository.delete(playlist_id)

    @staticmethod
    async def add_track_to_playlist(playlist_id: int, youtube_id: str) -> dict:
        playlist = await PlaylistRepository.get_by_id(playlist_id)
        if not playlist:
            raise NotFoundException("Playlist not found")

        track = await TrackRepository.get_by_youtube_id(youtube_id)
        if not track:
            raise NotFoundException(f"Track {youtube_id} not found in database")

        existing_tracks = await PlaylistRepository.get_tracks(playlist_id)
        if any(t["youtube_id"] == youtube_id for t in existing_tracks):
            raise ConflictException("Track already in playlist")

        return await PlaylistRepository.add_track(playlist_id, track["id"])

    @staticmethod
    async def remove_track_from_playlist(playlist_id: int, youtube_id: str) -> bool:
        track = await TrackRepository.get_by_youtube_id(youtube_id)
        if not track:
            raise NotFoundException(f"Track {youtube_id} not found")
        return await PlaylistRepository.remove_track(playlist_id, track["id"])

    @staticmethod
    async def reorder_tracks(playlist_id: int, track_ids: list[int]) -> None:
        playlist = await PlaylistRepository.get_by_id(playlist_id)
        if not playlist:
            raise NotFoundException("Playlist not found")
        await PlaylistRepository.reorder_tracks(playlist_id, track_ids)


playlist_service = PlaylistService()
