package com.ultrapuremusic.feature.search

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import com.ultrapuremusic.domain.usecase.search.SearchSongsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

val MUSIC_GENRES = listOf(
    "Nhạc trẻ", "Ballad", "V-Pop", "K-Pop", "EDM", "Rock", "Pop", "Rap", "Jazz", "Cổ điển", "Indie", "R&B",
)

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<Song> = emptyList(),
    val error: String? = null,
    val hasSearched: Boolean = false,
    val selectedGenre: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchSongsUseCase: SearchSongsUseCase,
    private val playSongUseCase: PlaySongUseCase
) : BaseViewModel<SearchUiState>(SearchUiState()) {

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        updateState { copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank() && uiState.value.selectedGenre == null) {
            updateState { copy(results = emptyList(), hasSearched = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            search(uiState.value.query, uiState.value.selectedGenre)
        }
    }

    fun setGenre(genre: String?) {
        val new = if (uiState.value.selectedGenre == genre) null else genre
        updateState { copy(selectedGenre = new) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            search(uiState.value.query, new)
        }
    }

    private suspend fun search(query: String, genre: String?) {
        val effectiveQuery = buildString {
            if (query.isNotBlank()) append(query.trim())
            if (genre != null) { if (isNotEmpty()) append(" "); append(genre) }
        }
        if (effectiveQuery.isBlank()) return
        updateState { copy(isSearching = true, error = null) }
        when (val result = searchSongsUseCase(effectiveQuery)) {
            is ResultWrapper.Success -> updateState {
                copy(results = result.data, isSearching = false, hasSearched = true)
            }
            is ResultWrapper.Error -> updateState {
                copy(error = result.message, isSearching = false, hasSearched = true)
            }
            is ResultWrapper.Loading -> Unit
        }
    }

    fun playSong(song: Song) {
        launch { playSongUseCase(song) }
    }

    fun clearSearch() {
        updateState { SearchUiState() }
        searchJob?.cancel()
    }
}
