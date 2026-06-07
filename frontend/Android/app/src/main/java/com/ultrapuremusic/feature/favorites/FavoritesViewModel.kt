package com.ultrapuremusic.feature.favorites

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.toModel
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.PlayerRepository
import com.ultrapuremusic.domain.usecase.favorites.AddFavoriteUseCase
import com.ultrapuremusic.domain.usecase.favorites.RemoveFavoriteUseCase
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class FavoritesUiState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val playerRepository: PlayerRepository,
) : BaseViewModel<FavoritesUiState>(FavoritesUiState()) {

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        combine(
            favoriteDao.getAllFavoriteSongIds(),
            songDao.getAllSongs()
        ) { favoriteIds, allSongs ->
            val favoriteSet = favoriteIds.toSet()
            allSongs.filter { it.id in favoriteSet }.map { it.toModel() }
        }.onEach { songs ->
            updateState { copy(songs = songs, isLoading = false) }
        }.launchIn(viewModelScope)
    }

    fun removeFavorite(songId: String) {
        launch { removeFavoriteUseCase(songId) }
    }

    fun playSong(song: Song) {
        launch { playSongUseCase(song) }
    }

    /** Play all favorited songs starting from [startIndex] (default 0 = first). */
    fun playAll(startIndex: Int = 0) {
        val songs = uiState.value.songs
        if (songs.isEmpty()) return
        playerRepository.setQueue(songs, startIndex.coerceIn(0, songs.lastIndex))
    }
}
