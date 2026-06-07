package com.ultrapuremusic.core.player

import com.ultrapuremusic.data.model.Song

/**
 * Resolves a playable audio stream URL for a [Song] (via the backend / yt-dlp).
 *
 * Lives in the player layer so [AudioPlayerManager] can lazily resolve each song
 * right before it plays — important for queue/playlist playback where resolving
 * every track upfront would be slow.
 */
interface StreamUrlResolver {
    /** Returns the song with [Song.streamUrl] populated, or the original song on failure. */
    suspend fun resolve(song: Song): Song
}
