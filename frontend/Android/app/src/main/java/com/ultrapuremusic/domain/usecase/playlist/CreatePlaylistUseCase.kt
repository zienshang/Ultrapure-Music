package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.domain.repository.PlaylistRepository
import javax.inject.Inject

class CreatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(name: String, description: String? = null): ResultWrapper<Playlist> {
        if (name.isBlank()) return ResultWrapper.Error("Playlist name cannot be empty")
        return playlistRepository.createPlaylist(name.trim(), description?.trim())
    }
}
