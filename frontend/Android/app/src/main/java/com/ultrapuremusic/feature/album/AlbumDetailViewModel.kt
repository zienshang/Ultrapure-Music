package com.ultrapuremusic.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.toModel
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumName: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songDao: SongDao,
    private val playSongUseCase: PlaySongUseCase,
) : BaseViewModel<AlbumDetailUiState>(AlbumDetailUiState()) {

    private val albumName: String =
        android.net.Uri.decode(savedStateHandle.get<String>("albumName") ?: "")

    init {
        updateState { copy(albumName = albumName) }
        songDao.getSongsByAlbum(albumName)
            .onEach { entities ->
                updateState { copy(songs = entities.map { it.toModel() }, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun playSong(song: Song) = launch { playSongUseCase(song) }

    fun playAll() {
        val songs = uiState.value.songs
        if (songs.isEmpty()) return
        launch { playSongUseCase(songs.first()) }
    }
}
