package com.ultrapuremusic.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/**
 * Stateless utility that writes playback state to SharedPreferences and
 * notifies [MusicWidget] to redraw.
 *
 * Used from two sites:
 * - [com.ultrapuremusic.core.player.AudioPlayerManager] (inside the app process, via DI context)
 * - [MusicWidget] itself when reading current state on widget redraw
 */
class WidgetUpdater(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun update(title: String, artist: String, isPlaying: Boolean, artUrl: String?) {
        prefs.edit {
            putString(KEY_TITLE,   title)
            putString(KEY_ARTIST,  artist)
            putBoolean(KEY_PLAYING, isPlaying)
            putString(KEY_ART_URL, artUrl)
        }
        broadcastWidgetUpdate()
    }

    fun readState(): WidgetState = WidgetState(
        title     = prefs.getString(KEY_TITLE,   "") ?: "",
        artist    = prefs.getString(KEY_ARTIST,  "") ?: "",
        isPlaying = prefs.getBoolean(KEY_PLAYING, false),
        artUrl    = prefs.getString(KEY_ART_URL, null),
    )

    private fun broadcastWidgetUpdate() {
        val awm = AppWidgetManager.getInstance(context)
        val ids = awm.getAppWidgetIds(ComponentName(context, MusicWidget::class.java))
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, MusicWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        )
    }

    companion object {
        const val PREFS_NAME = "widget_state"
        const val KEY_TITLE   = "title"
        const val KEY_ARTIST  = "artist"
        const val KEY_PLAYING = "is_playing"
        const val KEY_ART_URL = "art_url"
    }
}

data class WidgetState(
    val title: String,
    val artist: String,
    val isPlaying: Boolean,
    val artUrl: String?,
)
