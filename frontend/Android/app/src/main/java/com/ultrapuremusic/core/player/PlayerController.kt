package com.ultrapuremusic.core.player

import com.ultrapuremusic.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerController @Inject constructor(
    private val audioPlayerManager: AudioPlayerManager
) {
    val state get() = audioPlayerManager.playerState

    fun handle(event: PlayerEvent) {
        when (event) {
            is PlayerEvent.Play -> audioPlayerManager.playSong(event.song)
            is PlayerEvent.Pause -> audioPlayerManager.pause()
            is PlayerEvent.Resume -> audioPlayerManager.resume()
            is PlayerEvent.Seek -> audioPlayerManager.seekTo(event.positionMs)
            is PlayerEvent.SkipNext -> audioPlayerManager.skipToNext()
            is PlayerEvent.SkipPrev -> audioPlayerManager.skipToPrev()
            is PlayerEvent.SetQueue -> audioPlayerManager.setQueue(event.songs, event.startIndex)
            is PlayerEvent.AddToQueue      -> audioPlayerManager.addToQueue(event.song)
            is PlayerEvent.PlayNext        -> audioPlayerManager.playNext(event.song)
            is PlayerEvent.JumpToQueueIndex -> audioPlayerManager.jumpToQueueIndex(event.index)
            is PlayerEvent.RemoveFromQueue -> audioPlayerManager.removeFromQueue(event.index)
            is PlayerEvent.SetPlaybackSpeed -> audioPlayerManager.setPlaybackSpeed(event.speed)
            is PlayerEvent.SetSleepTimer   -> audioPlayerManager.setSleepTimer(event.durationMs)
            is PlayerEvent.CancelSleepTimer -> audioPlayerManager.cancelSleepTimer()
            is PlayerEvent.ToggleShuffle   -> audioPlayerManager.toggleShuffle()
            is PlayerEvent.ToggleRepeat    -> audioPlayerManager.toggleRepeat()
            is PlayerEvent.Release         -> audioPlayerManager.release()
        }
    }
}
