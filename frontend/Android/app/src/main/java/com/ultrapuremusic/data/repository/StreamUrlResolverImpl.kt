package com.ultrapuremusic.data.repository

import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.core.player.StreamUrlResolver
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreamUrlResolverImpl @Inject constructor(
    private val musicRepository: MusicRepository,
    private val userPreferences: UserPreferences,
) : StreamUrlResolver {

    override suspend fun resolve(song: Song): Song {
        // Skip network call if URL was pre-fetched (gapless pre-fetch optimisation).
        if (!song.streamUrl.isNullOrBlank()) return song
        val youtubeId = song.youtubeId ?: song.id
        val quality = userPreferences.audioQuality.first()
        return when (val result = musicRepository.getSongStreamUrl(youtubeId, quality)) {
            is ResultWrapper.Success -> song.copy(streamUrl = result.data)
            is ResultWrapper.Error -> {
                Timber.w("Stream URL resolution failed for %s: %s", youtubeId, result.message)
                song
            }
            else -> song
        }
    }
}
