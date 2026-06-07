package com.ultrapuremusic.feature.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.toModel
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.PlayerRepository
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import com.ultrapuremusic.domain.usecase.search.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class ArtistDetailUiState(
    val artistName: String = "",
    val cachedSongs: List<Song> = emptyList(),    // offline-first from Room
    val remoteSongs: List<Song> = emptyList(),    // live results from backend search
    val isLoadingRemote: Boolean = false,
    val error: String? = null,
) {
    /** Merged, deduplicated list: cached first (they have stream URLs), then remote extras. */
    val allSongs: List<Song> get() {
        val cachedIds = cachedSongs.map { it.id }.toSet()
        return cachedSongs + remoteSongs.filterNot { it.id in cachedIds }
    }
}

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    private val searchSongsUseCase: SearchSongsUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<ArtistDetailUiState>(ArtistDetailUiState()) {

    private val artistName: String =
        android.net.Uri.decode(savedStateHandle.get<String>("artistName") ?: "")

    init {
        updateState { copy(artistName = artistName) }
        loadCached()
        loadRemote()
    }

    private fun loadCached() {
        songDao.getSongsByArtist(artistName)
            .onEach { entities ->
                updateState { copy(cachedSongs = entities.map { it.toModel() }) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadRemote() {
        if (artistName.isBlank()) return
        launch {
            updateState { copy(isLoadingRemote = true, error = null) }
            val result = searchSongsUseCase(artistName)
            when (result) {
                is com.ultrapuremusic.core.util.ResultWrapper.Success -> updateState {
                    copy(remoteSongs = result.data, isLoadingRemote = false)
                }
                is com.ultrapuremusic.core.util.ResultWrapper.Error -> updateState {
                    copy(error = result.message, isLoadingRemote = false)
                }
                else -> Unit
            }
        }
    }

    /** Play a single song (tapped from the list). */
    fun playSong(song: Song) {
        val songs = uiState.value.allSongs
        val index = songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playerRepository.setQueue(songs, index)
    }

    /** Queue all artist songs starting from the first track. */
    fun playAll(startIndex: Int = 0) {
        val songs = uiState.value.allSongs
        if (songs.isEmpty()) return
        playerRepository.setQueue(songs, startIndex.coerceIn(0, songs.lastIndex))
    }

    fun refresh() = loadRemote()
}
