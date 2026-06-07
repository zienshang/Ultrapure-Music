package com.ultrapuremusic.domain.repository

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    fun getAllPlaylists(): Flow<List<Playlist>>
    /** Fetch playlists from the backend and cache them into the local Room DB. */
    suspend fun refreshPlaylists(): ResultWrapper<Unit>
    suspend fun getPlaylistById(id: String): ResultWrapper<Playlist>
    /** Fetch full playlist detail (incl. its tracks) from the backend. */
    suspend fun getPlaylistDetail(id: String): ResultWrapper<Playlist>
    suspend fun createPlaylist(name: String, description: String?): ResultWrapper<Playlist>
    suspend fun deletePlaylist(id: String): ResultWrapper<Unit>
    suspend fun addSongToPlaylist(playlistId: String, song: Song): ResultWrapper<Unit>
    suspend fun removeSongFromPlaylist(playlistId: String, songId: String): ResultWrapper<Unit>
    suspend fun syncYoutubePlaylist(youtubePlaylistId: String): ResultWrapper<Playlist>
    suspend fun getYoutubePlaylists(): ResultWrapper<List<Playlist>>
    suspend fun renamePlaylist(id: String, name: String): ResultWrapper<Unit>
    /** Persist a new track order for a local-only playlist. */
    suspend fun reorderTracks(playlistId: String, orderedSongIds: List<String>): ResultWrapper<Unit>
}
