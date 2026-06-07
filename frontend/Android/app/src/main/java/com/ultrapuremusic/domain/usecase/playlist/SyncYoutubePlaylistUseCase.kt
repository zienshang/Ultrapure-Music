package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.domain.repository.PlaylistRepository
import javax.inject.Inject

class SyncYoutubePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(youtubePlaylistId: String): ResultWrapper<Playlist> =
        playlistRepository.syncYoutubePlaylist(youtubePlaylistId)
}
