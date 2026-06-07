package com.ultrapuremusic.domain.usecase.playlist

import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    operator fun invoke(): Flow<List<Playlist>> = playlistRepository.getAllPlaylists()
}
