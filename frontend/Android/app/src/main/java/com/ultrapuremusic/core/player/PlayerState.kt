package com.ultrapuremusic.core.player

import com.ultrapuremusic.data.model.Song

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val queue: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val playbackSpeed: Float = 1.0f,
    /** Epoch millis when the sleep timer should fire; 0 = no timer active. */
    val sleepTimerEndMs: Long = 0L,
    val error: String? = null,
) {
    val hasNext: Boolean get() = currentIndex < queue.size - 1 || repeatMode != RepeatMode.NONE
    val hasPrev: Boolean get() = currentIndex > 0 || repeatMode != RepeatMode.NONE
    val progress: Float get() = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val isSleepTimerActive: Boolean get() = sleepTimerEndMs > 0L
}

enum class RepeatMode { NONE, ONE, ALL }
