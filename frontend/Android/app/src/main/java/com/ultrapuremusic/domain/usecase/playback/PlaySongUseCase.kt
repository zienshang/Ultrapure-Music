package com.ultrapuremusic.domain.usecase.playback

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import com.ultrapuremusic.domain.repository.PlayerRepository
import timber.log.Timber
import javax.inject.Inject

/**
 * Plays a song via ExoPlayer.
 *
 * If [Song.streamUrl] is null (e.g. from search results), it first resolves a real
 * audio URL from the backend (/api/v1/player/stream) before handing off to the player.
 */
class PlaySongUseCase @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val musicRepository: MusicRepository,
) {
    suspend operator fun invoke(song: Song) {
        val songWithUrl = if (song.streamUrl.isNullOrBlank()) {
            val youtubeId = song.youtubeId ?: song.id
            Timber.d("Resolving stream URL for %s", youtubeId)
            val streamResult = musicRepository.getSongStreamUrl(youtubeId)
            when (streamResult) {
                is ResultWrapper.Success -> song.copy(streamUrl = streamResult.data)
                is ResultWrapper.Error -> {
                    Timber.w("Stream URL resolution failed for %s: %s", youtubeId, streamResult.message)
                    song // play with null URL — player will surface an error state
                }
                else -> song
            }
        } else {
            song
        }
        playerRepository.playSong(songWithUrl)
    }
}
