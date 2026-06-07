package com.ultrapuremusic.domain.usecase.playback

import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.PlayerRepository
import javax.inject.Inject

/**
 * Plays an entire playlist by loading it into the player queue and starting
 * playback from [startIndex]. Stream URLs are resolved lazily per-track by the
 * player, so this returns immediately.
 */
class PlayPlaylistUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
) {
    operator fun invoke(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        playerRepository.setQueue(songs, startIndex.coerceIn(0, songs.size - 1))
    }
}
