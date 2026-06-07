package com.ultrapuremusic.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import com.ultrapuremusic.domain.repository.PlayerRepository
import com.ultrapuremusic.domain.usecase.favorites.AddFavoriteUseCase
import com.ultrapuremusic.domain.usecase.favorites.RemoveFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity-independent VM that drives the [SongOptionsSheetHost] from any screen.
 *
 * Usage:
 *   val actionsVm: SongActionsViewModel = hiltViewModel()
 *   SongCard(..., onMoreClick = { actionsVm.showOptions(song) })
 *   SongOptionsSheetHost(actionsVm, onNavigateToPlayer = ...)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SongActionsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val musicRepository: MusicRepository,
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao,
) : ViewModel() {

    private val _selectedSong = MutableStateFlow<Song?>(null)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    /** Live favorite status for the song currently shown in the options sheet. */
    val isSelectedFavorite: StateFlow<Boolean> = _selectedSong
        .flatMapLatest { song ->
            if (song == null) flowOf(false) else favoriteDao.isFavorite(song.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * Live downloaded status for the song currently shown in the options sheet.
     * Reads from songs table (not from the in-memory Song.isDownloaded field,
     * which can be stale because the Song instance was captured before the
     * download finished).
     */
    val isSelectedDownloaded: StateFlow<Boolean> = _selectedSong
        .flatMapLatest { song ->
            if (song == null) flowOf(false)
            else songDao.observeSongById(song.id).map { it?.isDownloaded == true }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Show the options sheet for [song]. */
    fun showOptions(song: Song) { _selectedSong.value = song }

    /** Dismiss the options sheet. */
    fun dismissOptions() { _selectedSong.value = null }

    // ── Playback actions ─────────────────────────────────────────────────────

    fun playNow(song: Song)      = playerRepository.playSong(song)
    fun playNext(song: Song)     = playerRepository.playNext(song)
    fun addToQueue(song: Song)   = playerRepository.addToQueue(song)

    // ── Favorite ─────────────────────────────────────────────────────────────

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            if (isSelectedFavorite.value) removeFavoriteUseCase(song.id)
            else addFavoriteUseCase(song)   // pass full Song so it's upserted to songs table
        }
    }

    // ── Download ─────────────────────────────────────────────────────────────

    fun downloadSong(song: Song) {
        viewModelScope.launch { musicRepository.downloadSong(song) }
    }

    /** Remove the local file + flip is_downloaded = false. */
    fun deleteDownload(song: Song) {
        viewModelScope.launch { musicRepository.deleteDownload(song.id) }
    }
}
