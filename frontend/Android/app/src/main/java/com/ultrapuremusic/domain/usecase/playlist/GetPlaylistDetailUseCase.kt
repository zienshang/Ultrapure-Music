package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.domain.repository.PlaylistRepository
import javax.inject.Inject

/**
 * Loads a single playlist with its full track list from the backend.
 */
class GetPlaylistDetailUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(id: String): ResultWrapper<Playlist> =
        playlistRepository.getPlaylistDetail(id)
}
