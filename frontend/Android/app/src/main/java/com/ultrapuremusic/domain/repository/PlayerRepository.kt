package com.ultrapuremusic.domain.repository

import com.ultrapuremusic.core.player.PlayerState
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.flow.StateFlow

interface PlayerRepository {
    val playerState: StateFlow<PlayerState>
    fun playSong(song: Song)
    fun pause()
    fun resume()
    fun seekTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrev()
    fun setQueue(songs: List<Song>, startIndex: Int = 0)
    fun addToQueue(song: Song)
    fun playNext(song: Song)
    fun jumpToQueueIndex(index: Int)
    fun removeFromQueue(index: Int)
    fun setPlaybackSpeed(speed: Float)
    fun setSleepTimer(durationMs: Long)
    fun cancelSleepTimer()
    fun toggleShuffle()
    fun toggleRepeat()
    fun release()
}
