package com.ultrapuremusic.feature.downloads

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.PlaylistDao
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.usecase.cache.DownloadSongUseCase
import com.ultrapuremusic.domain.usecase.cache.GetOfflineSongsUseCase
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/** A playlist with at least one downloaded track + offline progress numbers. */
data class DownloadedPlaylist(
    val playlist: Playlist,
    val downloadedCount: Int,
    val totalCount: Int,
) {
    val isFullyDownloaded: Boolean
        get() = totalCount > 0 && downloadedCount >= totalCount
}

data class DownloadsUiState(
    val songs: List<Song> = emptyList(),
    val playlists: List<DownloadedPlaylist> = emptyList(),
    val downloadingIds: Set<String> = emptySet(),
    val error: String? = null,
)

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val getOfflineSongsUseCase: GetOfflineSongsUseCase,
    private val downloadSongUseCase: DownloadSongUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playlistDao: PlaylistDao,
) : BaseViewModel<DownloadsUiState>(DownloadsUiState()) {

    init {
        // Downloaded songs (reactive)
        getOfflineSongsUseCase()
            .onEach { songs -> updateState { copy(songs = songs) } }
            .launchIn(viewModelScope)

        // Downloaded playlists (reactive)
        playlistDao.getPlaylistsWithDownloads()
            .onEach { rows ->
                val mapped = rows.map { row ->
                    DownloadedPlaylist(
                        playlist = Playlist(
                            id                = row.playlist.id,
                            name              = row.playlist.name,
                            description       = row.playlist.description,
                            thumbnailUrl      = row.playlist.thumbnailUrl,
                            songCount         = row.totalCount,
                            youtubePlaylistId = row.playlist.youtubePlaylistId,
                            isSynced          = row.playlist.isSynced,
                            createdAt         = row.playlist.createdAt,
                            updatedAt         = row.playlist.updatedAt,
                        ),
                        downloadedCount = row.downloadedCount,
                        totalCount      = row.totalCount,
                    )
                }
                updateState { copy(playlists = mapped) }
            }
            .launchIn(viewModelScope)
    }

    fun downloadSong(song: Song) {
        launch {
            updateState { copy(downloadingIds = downloadingIds + song.id) }
            when (val result = downloadSongUseCase(song)) {
                is ResultWrapper.Success -> updateState {
                    copy(downloadingIds = downloadingIds - song.id)
                }
                is ResultWrapper.Error -> updateState {
                    copy(error = result.message, downloadingIds = downloadingIds - song.id)
                }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun playSong(song: Song) {
        launch { playSongUseCase(song) }
    }
}
