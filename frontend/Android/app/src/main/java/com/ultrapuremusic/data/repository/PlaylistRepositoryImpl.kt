package com.ultrapuremusic.data.repository

import com.ultrapuremusic.core.database.dao.PlaylistDao
import com.ultrapuremusic.core.database.entity.PlaylistEntity
import com.ultrapuremusic.core.database.entity.PlaylistSongCrossRef
import com.ultrapuremusic.core.datastore.TokenManager
import com.ultrapuremusic.core.network.api.BackendApiService
import com.ultrapuremusic.core.network.dto.BackendPlaylistResponse
import com.ultrapuremusic.core.network.dto.BackendTrackResponse
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.core.util.safeApiCall
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.data.remote.YoutubeRemoteDataSource
import com.ultrapuremusic.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val youtubeRemoteDataSource: YoutubeRemoteDataSource,
    private val backendApiService: BackendApiService,
    private val tokenManager: TokenManager,
) : PlaylistRepository {

    override fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { it.toPlaylist() }
        }

    override suspend fun refreshPlaylists(): ResultWrapper<Unit> = safeApiCall {
        val remote = backendApiService.getPlaylists()
        val entities = remote.map { it.toEntity() }
        playlistDao.insertPlaylists(entities)
    }

    override suspend fun getPlaylistById(id: String): ResultWrapper<Playlist> {
        val entity = playlistDao.getPlaylistById(id)
            ?: return ResultWrapper.Error("Playlist not found")
        return ResultWrapper.Success(entity.toPlaylist())
    }

    override suspend fun getPlaylistDetail(id: String): ResultWrapper<Playlist> = safeApiCall {
        val backendId = id.toIntOrNull()
        if (backendId != null) {
            val detail = backendApiService.getPlaylist(backendId)
            val songs  = detail.tracks.map { it.toSong() }
            return@safeApiCall Playlist(
                id                = detail.id.toString(),
                name              = detail.name,
                description       = detail.description,
                thumbnailUrl      = songs.firstOrNull()?.thumbnailUrl,
                songCount         = songs.size,
                songs             = songs,
                youtubePlaylistId = detail.youtubePlaylistId,
                isSynced          = detail.isSynced,
            )
        }

        // Non-numeric id → either a public YouTube playlist (e.g. "PLxxx...") or a
        // locally-created one (UUID). Try YouTube first when we have a valid token.
        if (looksLikeYoutubePlaylistId(id) && tokenManager.isYoutubeTokenValid()) {
            val token = tokenManager.getYoutubeToken()
            if (!token.isNullOrBlank()) {
                val tracks = backendApiService.getYoutubePlaylistItems(
                    playlistId = id, accessToken = token, limit = 50,
                )
                val songs = tracks.map { it.toSong() }
                return@safeApiCall Playlist(
                    id                = id,
                    name              = "Playlist",
                    description       = null,
                    thumbnailUrl      = songs.firstOrNull()?.thumbnailUrl,
                    songCount         = songs.size,
                    songs             = songs,
                    youtubePlaylistId = id,
                    isSynced          = false,
                )
            }
        }

        // Fallback: locally-created playlist (UUID) lookup from Room.
        playlistDao.getPlaylistById(id)?.toPlaylist()
            ?: throw IllegalStateException("Playlist not found")
    }

    private fun looksLikeYoutubePlaylistId(id: String): Boolean =
        id.startsWith("PL") || id.startsWith("UU") || id.startsWith("FL") ||
        id.startsWith("LL") || id.startsWith("RD") || id.startsWith("OL")

    override suspend fun createPlaylist(name: String, description: String?): ResultWrapper<Playlist> {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = id, name = name, description = description,
            thumbnailUrl = null, youtubePlaylistId = null, isSynced = false,
            trackCount = 0, createdAt = now, updatedAt = now
        )
        playlistDao.insertPlaylist(entity)
        return ResultWrapper.Success(entity.toPlaylist())
    }

    override suspend fun deletePlaylist(id: String): ResultWrapper<Unit> {
        playlistDao.clearPlaylistSongs(id)
        playlistDao.deletePlaylist(id)
        return ResultWrapper.Success(Unit)
    }

    override suspend fun addSongToPlaylist(playlistId: String, song: Song): ResultWrapper<Unit> {
        val crossRef = PlaylistSongCrossRef(playlistId = playlistId, songId = song.id)
        playlistDao.insertPlaylistSongCrossRef(crossRef)
        // Auto-set thumbnail from the first song added (no-op if one already exists)
        song.thumbnailUrl?.let { url ->
            if (url.isNotBlank()) playlistDao.setThumbnailIfEmpty(playlistId, url)
        }
        return ResultWrapper.Success(Unit)
    }

    override suspend fun removeSongFromPlaylist(playlistId: String, songId: String): ResultWrapper<Unit> {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        return ResultWrapper.Success(Unit)
    }

    override suspend fun syncYoutubePlaylist(youtubePlaylistId: String): ResultWrapper<Playlist> =
        safeApiCall { youtubeRemoteDataSource.syncPlaylist(youtubePlaylistId) }

    override suspend fun getYoutubePlaylists(): ResultWrapper<List<Playlist>> =
        safeApiCall { youtubeRemoteDataSource.getUserPlaylists() }

    private fun PlaylistEntity.toPlaylist() = Playlist(
        id = id, name = name, description = description,
        thumbnailUrl = thumbnailUrl, youtubePlaylistId = youtubePlaylistId,
        songCount = trackCount,
        isSynced = isSynced, createdAt = createdAt, updatedAt = updatedAt
    )

    private fun BackendTrackResponse.toSong() = Song(
        id           = youtubeId.ifEmpty { id?.toString() ?: "" },
        title        = title,
        artist       = artist,
        thumbnailUrl = thumbnailUrl,
        youtubeId    = youtubeId,
        durationMs   = duration * 1000L,
    )

    override suspend fun renamePlaylist(id: String, name: String): ResultWrapper<Unit> {
        return try {
            playlistDao.renamePlaylist(id, name.trim())
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e.message ?: "Rename failed")
        }
    }

    override suspend fun reorderTracks(
        playlistId: String,
        orderedSongIds: List<String>,
    ): ResultWrapper<Unit> {
        return try {
            val refs = orderedSongIds.mapIndexed { index, songId ->
                PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = index)
            }
            playlistDao.replacePlaylistSongs(playlistId, refs)
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e.message ?: "Reorder failed")
        }
    }

    private fun BackendPlaylistResponse.toEntity(): PlaylistEntity {
        val now = System.currentTimeMillis()
        return PlaylistEntity(
            id                = id.toString(),
            name              = name,
            description       = description,
            thumbnailUrl      = thumbnailUrl,
            youtubePlaylistId = youtubePlaylistId,
            isSynced          = isSynced,
            trackCount        = trackCount,
            // Backend timestamps are ISO strings; local ordering only needs a
            // monotonic Long, so reuse current time (newest sync = top).
            createdAt         = now,
            updatedAt         = now,
        )
    }
}
