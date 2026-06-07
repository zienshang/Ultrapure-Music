package com.ultrapuremusic.feature.home

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import com.ultrapuremusic.domain.usecase.playback.PlaySongUseCase
import com.ultrapuremusic.domain.usecase.playlist.GetPlaylistUseCase
import com.ultrapuremusic.domain.usecase.playlist.RefreshPlaylistsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val trendingSongs: List<Song> = emptyList(),
    val popularSongs: List<Song> = emptyList(),
    val youtubeFeedSongs: List<Song> = emptyList(),
    val recommendedSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val communityPlaylists: List<Playlist> = emptyList(),
    val isLoadingRecommendations: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val getPlaylistUseCase: GetPlaylistUseCase,
    private val refreshPlaylistsUseCase: RefreshPlaylistsUseCase,
    private val playSongUseCase: PlaySongUseCase
) : BaseViewModel<HomeUiState>(HomeUiState()) {

    init {
        loadContent()
    }

    private fun loadContent() {
        launch {
            updateState { copy(isLoading = true) }
            musicRepository.getTrendingSongs().collect { result ->
                when (result) {
                    is ResultWrapper.Success -> updateState { copy(trendingSongs = result.data, isLoading = false) }
                    is ResultWrapper.Error -> updateState { copy(error = result.message, isLoading = false) }
                    is ResultWrapper.Loading -> updateState { copy(isLoading = true) }
                }
            }
        }

        // Public playlists from the YouTube community ('Danh sách từ mọi người tạo').
        launch {
            when (val result = musicRepository.getCommunityPlaylists()) {
                is ResultWrapper.Success -> updateState { copy(communityPlaylists = result.data) }
                else -> Unit
            }
        }

        // Feed from the user's YouTube account (subscriptions). Empty if not connected.
        launch {
            when (val result = musicRepository.getYoutubeAccountFeed()) {
                is ResultWrapper.Success -> updateState { copy(youtubeFeedSongs = result.data) }
                else -> Unit
            }
        }

        // Community-popular tracks ('Thịnh hành với người nghe').
        launch {
            when (val result = musicRepository.getPopularTracks()) {
                is ResultWrapper.Success -> updateState { copy(popularSongs = result.data) }
                else -> Unit
            }
        }

        // Personalized recommendations.
        launch {
            updateState { copy(isLoadingRecommendations = true) }
            when (val result = musicRepository.getRecommendations()) {
                is ResultWrapper.Success ->
                    updateState { copy(recommendedSongs = result.data, isLoadingRecommendations = false) }
                is ResultWrapper.Error ->
                    updateState { copy(isLoadingRecommendations = false) }
                is ResultWrapper.Loading -> Unit
            }
        }

        musicRepository.getRecentlyPlayed()
            .onEach { songs -> updateState { copy(recentlyPlayed = songs) } }
            .launchIn(viewModelScope)

        // Reactive playlists from Room…
        getPlaylistUseCase()
            .onEach { playlists -> updateState { copy(playlists = playlists) } }
            .launchIn(viewModelScope)
        // …and pull the latest from the backend into Room.
        launch { refreshPlaylistsUseCase() }
    }

    fun playSong(song: Song) {
        launch { playSongUseCase(song) }
    }

    fun refresh() = loadContent()
}
