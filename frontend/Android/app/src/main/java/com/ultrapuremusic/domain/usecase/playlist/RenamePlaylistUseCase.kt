package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.domain.repository.PlaylistRepository
import javax.inject.Inject

class RenamePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) {
    suspend operator fun invoke(id: String, name: String): ResultWrapper<Unit> =
        playlistRepository.renamePlaylist(id, name)
}
