package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Pulls the latest playlists from the backend and caches them into the local
 * Room DB. The reactive [GetPlaylistUseCase] flow then emits the fresh data.
 */
class RefreshPlaylistsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(): ResultWrapper<Unit> =
        playlistRepository.refreshPlaylists()
}
