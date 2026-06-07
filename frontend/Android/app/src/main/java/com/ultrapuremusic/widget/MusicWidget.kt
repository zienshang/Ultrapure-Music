package com.ultrapuremusic.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ultrapuremusic.MainActivity
import com.ultrapuremusic.R

/**
 * Home screen widget — shows current song title/artist and play/pause/skip controls.
 *
 * State is read from [WidgetUpdater]'s SharedPreferences, which [AudioPlayerManager]
 * writes every time playback changes. Artwork is loaded asynchronously and shown in
 * [widget_art]; falls back to the music-note placeholder while loading.
 */
class MusicWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val updater = WidgetUpdater(context)
        val state   = updater.readState()
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id, state)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            awm: AppWidgetManager,
            widgetId: Int,
            state: WidgetState,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_music)

            // Song info
            views.setTextViewText(R.id.widget_title,
                state.title.ifBlank { context.getString(R.string.app_name) })
            views.setTextViewText(R.id.widget_artist, state.artist)

            // Play/Pause icon
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
            )

            // PendingIntents for each button
            views.setOnClickPendingIntent(R.id.widget_root,   openAppIntent(context))
            views.setOnClickPendingIntent(R.id.widget_prev,   actionIntent(context, WidgetActionReceiver.ACTION_PREV))
            views.setOnClickPendingIntent(R.id.widget_play_pause, actionIntent(context, WidgetActionReceiver.ACTION_PLAY_PAUSE))
            views.setOnClickPendingIntent(R.id.widget_next,   actionIntent(context, WidgetActionReceiver.ACTION_NEXT))

            awm.updateAppWidget(widgetId, views)
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        private fun actionIntent(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                Intent(context, WidgetActionReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
