package com.ultrapuremusic.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ultrapuremusic.core.player.AudioPlayerManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives play/pause/skip intents sent by the home screen widget buttons.
 * Hilt injects [AudioPlayerManager] so widget controls are wired to live playback.
 */
@AndroidEntryPoint
class WidgetActionReceiver : BroadcastReceiver() {

    @Inject lateinit var audioPlayerManager: AudioPlayerManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PLAY_PAUSE -> {
                val state = audioPlayerManager.playerState.value
                if (state.isPlaying) audioPlayerManager.pause()
                else                 audioPlayerManager.resume()
            }
            ACTION_PREV -> audioPlayerManager.skipToPrev()
            ACTION_NEXT -> audioPlayerManager.skipToNext()
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.ultrapuremusic.widget.PLAY_PAUSE"
        const val ACTION_PREV       = "com.ultrapuremusic.widget.PREV"
        const val ACTION_NEXT       = "com.ultrapuremusic.widget.NEXT"
    }
}
