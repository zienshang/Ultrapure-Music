package com.ultrapuremusic.feature.playlist

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.network.api.BackendApiService
import com.ultrapuremusic.core.network.dto.ImportPlaylistRequest
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.ultrapuremusic.domain.repository.PlaylistRepository
import com.ultrapuremusic.domain.usecase.cache.DownloadPlaylistUseCase
import com.ultrapuremusic.domain.usecase.cache.PlaylistDownloadProgress
import com.ultrapuremusic.domain.usecase.playlist.CreatePlaylistUseCase
import com.ultrapuremusic.domain.usecase.playlist.GetPlaylistDetailUseCase
import com.ultrapuremusic.domain.usecase.playlist.GetPlaylistUseCase
import com.ultrapuremusic.domain.usecase.playlist.RefreshPlaylistsUseCase
import com.ultrapuremusic.domain.usecase.playlist.RenamePlaylistUseCase
import com.ultrapuremusic.domain.usecase.playlist.SyncYoutubePlaylistUseCase
import com.ultrapuremusic.domain.usecase.playback.PlayPlaylistUseCase
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSyncing: Boolean = false,
    val isImporting: Boolean = false,
    /** null = not downloading; non-null = in progress or just finished */
    val downloadProgress: PlaylistDownloadProgress? = null,
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val getPlaylistUseCase: GetPlaylistUseCase,
    private val getPlaylistDetailUseCase: GetPlaylistDetailUseCase,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val refreshPlaylistsUseCase: RefreshPlaylistsUseCase,
    private val renamePlaylistUseCase: RenamePlaylistUseCase,
    private val downloadPlaylistUseCase: DownloadPlaylistUseCase,
    private val syncYoutubePlaylistUseCase: SyncYoutubePlaylistUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playPlaylistUseCase: PlayPlaylistUseCase,
    private val playlistRepository: PlaylistRepository,
    private val backendApiService: BackendApiService,
) : BaseViewModel<PlaylistUiState>(PlaylistUiState()) {

    private val _exportEvent = MutableSharedFlow<Pair<String, String>>() // name, content
    val exportEvent: SharedFlow<Pair<String, String>> = _exportEvent.asSharedFlow()

    init {
        getPlaylistUseCase()
            .onEach { playlists -> updateState { copy(playlists = playlists) } }
            .launchIn(viewModelScope)
    }

    fun refreshPlaylists() {
        launch {
            updateState { copy(isLoading = true) }
            when (val result = refreshPlaylistsUseCase()) {
                is ResultWrapper.Success -> updateState { copy(isLoading = false) }
                is ResultWrapper.Error   -> updateState { copy(error = result.message, isLoading = false) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun createPlaylist(name: String) {
        launch {
            updateState { copy(isLoading = true) }
            when (val result = createPlaylistUseCase(name)) {
                is ResultWrapper.Success -> updateState { copy(isLoading = false) }
                is ResultWrapper.Error   -> updateState { copy(error = result.message, isLoading = false) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun renamePlaylist(id: String, newName: String) {
        launch {
            when (val result = renamePlaylistUseCase(id, newName)) {
                is ResultWrapper.Success -> {
                    // Update the selectedPlaylist name in memory so the detail screen refreshes.
                    updateState {
                        copy(
                            selectedPlaylist = selectedPlaylist?.takeIf { it.id == id }
                                ?.copy(name = newName) ?: selectedPlaylist
                        )
                    }
                }
                is ResultWrapper.Error -> updateState { copy(error = result.message) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun deletePlaylist(id: String) {
        launch { playlistRepository.deletePlaylist(id) }
    }

    /**
     * Download every song in [songs] for offline playback.
     * Already-downloaded songs are skipped automatically.
     * Progress is streamed via [PlaylistUiState.downloadProgress].
     */
    fun downloadPlaylist(songs: List<Song>) {
        if (songs.isEmpty()) {
            emitError("Playlist trống, không có gì để tải")
            return
        }
        if (uiState.value.downloadProgress?.isDone == false) {
            emitError("Đang tải playlist khác — vui lòng đợi")
            return
        }
        launch {
            var last: com.ultrapuremusic.domain.usecase.cache.PlaylistDownloadProgress? = null
            downloadPlaylistUseCase(songs)
                .collect { progress ->
                    last = progress
                    updateState { copy(downloadProgress = progress) }
                }
            // Surface a completion summary so the user always knows the outcome,
            // even when the progress bar disappears (it's hidden once isDone).
            last?.let { final ->
                val msg = buildString {
                    when {
                        final.failed == 0 && final.downloaded == 0 ->
                            append("Tất cả ${final.skipped} bài đã có sẵn offline")
                        final.failed == 0 ->
                            append("Đã tải ${final.downloaded} bài thành công")
                        final.downloaded == 0 ->
                            append("Tải thất bại tất cả ${final.failed} bài — kiểm tra mạng/backend")
                        else ->
                            append("Đã tải ${final.downloaded} bài · ${final.failed} bài lỗi")
                    }
                    if (final.skipped > 0 && final.downloaded > 0)
                        append(" · ${final.skipped} đã có sẵn")
                }
                emitError(msg)
            }
        }
    }

    fun clearDownloadProgress() = updateState { copy(downloadProgress = null) }

    /**
     * Persist the new track order for [playlistId].
     * [songs] must be the fully reordered list (position = index in list).
     */
    fun reorderTracks(playlistId: String, songs: List<Song>) {
        launch {
            playlistRepository.reorderTracks(playlistId, songs.map { it.id })
            // Optimistically update the in-memory selectedPlaylist so the UI
            // reflects the new order immediately without reloading from DB.
            updateState {
                copy(
                    selectedPlaylist = selectedPlaylist?.takeIf { it.id == playlistId }
                        ?.copy(songs = songs)
                        ?: selectedPlaylist
                )
            }
        }
    }

    fun syncYoutubePlaylist(youtubeId: String) {
        launch {
            updateState { copy(isSyncing = true) }
            when (val result = syncYoutubePlaylistUseCase(youtubeId)) {
                is ResultWrapper.Success -> updateState { copy(isSyncing = false) }
                is ResultWrapper.Error   -> updateState { copy(error = result.message, isSyncing = false) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) = updateState { copy(selectedPlaylist = playlist) }

    fun loadPlaylistDetail(id: String) {
        launch {
            updateState { copy(isLoading = true) }
            when (val result = getPlaylistDetailUseCase(id)) {
                is ResultWrapper.Success ->
                    updateState { copy(selectedPlaylist = result.data, isLoading = false) }
                is ResultWrapper.Error ->
                    updateState { copy(error = result.message, isLoading = false) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun playSong(song: Song) = launch { playSongUseCase(song) }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) =
        playPlaylistUseCase(songs, startIndex)

    // ── Import / Export ──────────────────────────────────────────────────────

    fun importFromText(name: String, tracksText: String) {
        launch {
            updateState { copy(isImporting = true) }
            try {
                backendApiService.importPlaylistFromText(
                    ImportPlaylistRequest(name = name, tracksText = tracksText)
                )
                refreshPlaylistsUseCase()
                emitError("Đã nhập playlist \"$name\"")
            } catch (e: Exception) {
                emitError("Nhập thất bại: ${e.localizedMessage}")
            } finally {
                updateState { copy(isImporting = false) }
            }
        }
    }

    fun exportPlaylist(playlistId: String, playlistName: String) {
        launch {
            try {
                val id = playlistId.toIntOrNull() ?: return@launch
                val body = backendApiService.exportPlaylist(id)
                val text = body.string()
                _exportEvent.emit(playlistName to text)
            } catch (e: Exception) {
                emitError("Xuất thất bại: ${e.localizedMessage}")
            }
        }
    }
}
