package com.ultrapuremusic.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.SimpleBitmapLoader
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.ListenableFuture
import com.ultrapuremusic.core.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // Load remote artwork (HTTPS thumbnail URLs) for the notification and lock screen.
        // CacheBitmapLoader wraps SimpleBitmapLoader (HTTPS-capable) and caches
        // loaded bitmaps in memory so repeated artwork fetches are instant.
        val bitmapLoader = CacheBitmapLoader(SimpleBitmapLoader())

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setCallback(MusicServiceCallback())
            .setBitmapLoader(bitmapLoader)
            .build()

        // Explicit notification config — channel + notification ID from Constants.
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(Constants.NOTIFICATION_CHANNEL_ID)
                .setNotificationId(Constants.NOTIFICATION_ID)
                .build()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            // Release only the session here; ExoPlayer is owned by AudioPlayerManager
            // and released via its own release() call — double-releasing ExoPlayer
            // causes an IllegalStateException that kills the app process.
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    private inner class MusicServiceCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult = MediaSession.ConnectionResult.accept(
            MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS,
            MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS,
        )

        /** Allow any MediaItem to be added (needed for Android Auto / widget control). */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<androidx.media3.common.MediaItem>,
        ): ListenableFuture<MutableList<androidx.media3.common.MediaItem>> =
            com.google.common.util.concurrent.Futures.immediateFuture(mediaItems)
    }
}
