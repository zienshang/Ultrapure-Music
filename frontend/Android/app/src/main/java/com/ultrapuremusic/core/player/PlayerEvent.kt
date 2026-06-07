package com.ultrapuremusic.core.player

import com.ultrapuremusic.data.model.Song

sealed class PlayerEvent {
    data class Play(val song: Song) : PlayerEvent()
    data object Pause : PlayerEvent()
    data object Resume : PlayerEvent()
    data class Seek(val positionMs: Long) : PlayerEvent()
    data object SkipNext : PlayerEvent()
    data object SkipPrev : PlayerEvent()
    data class SetQueue(val songs: List<Song>, val startIndex: Int) : PlayerEvent()
    data class AddToQueue(val song: Song) : PlayerEvent()
    data class PlayNext(val song: Song) : PlayerEvent()
    data class JumpToQueueIndex(val index: Int) : PlayerEvent()
    data class RemoveFromQueue(val index: Int) : PlayerEvent()
    data class SetPlaybackSpeed(val speed: Float) : PlayerEvent()
    data class SetSleepTimer(val durationMs: Long) : PlayerEvent()
    data object CancelSleepTimer : PlayerEvent()
    data object ToggleShuffle : PlayerEvent()
    data object ToggleRepeat : PlayerEvent()
    data object Release : PlayerEvent()
}
