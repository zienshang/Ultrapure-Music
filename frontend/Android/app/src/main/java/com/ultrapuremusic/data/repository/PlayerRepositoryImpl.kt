package com.ultrapuremusic.data.repository

import com.ultrapuremusic.core.player.AudioPlayerManager
import com.ultrapuremusic.core.player.PlayerState
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.PlayerRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) : PlayerRepository {

    override val playerState: StateFlow<PlayerState> get() = audioPlayerManager.playerState

    override fun playSong(song: Song) = audioPlayerManager.playSong(song)
    override fun pause() = audioPlayerManager.pause()
    override fun resume() = audioPlayerManager.resume()
    override fun seekTo(positionMs: Long) = audioPlayerManager.seekTo(positionMs)
    override fun skipToNext() = audioPlayerManager.skipToNext()
    override fun skipToPrev() = audioPlayerManager.skipToPrev()
    override fun setQueue(songs: List<Song>, startIndex: Int) = audioPlayerManager.setQueue(songs, startIndex)
    override fun addToQueue(song: Song)         = audioPlayerManager.addToQueue(song)
    override fun playNext(song: Song)           = audioPlayerManager.playNext(song)
    override fun jumpToQueueIndex(index: Int)   = audioPlayerManager.jumpToQueueIndex(index)
    override fun removeFromQueue(index: Int)    = audioPlayerManager.removeFromQueue(index)
    override fun setPlaybackSpeed(speed: Float) = audioPlayerManager.setPlaybackSpeed(speed)
    override fun setSleepTimer(durationMs: Long) = audioPlayerManager.setSleepTimer(durationMs)
    override fun cancelSleepTimer()             = audioPlayerManager.cancelSleepTimer()
    override fun toggleShuffle()                = audioPlayerManager.toggleShuffle()
    override fun toggleRepeat()                 = audioPlayerManager.toggleRepeat()
    override fun release()                      = audioPlayerManager.release()
}
