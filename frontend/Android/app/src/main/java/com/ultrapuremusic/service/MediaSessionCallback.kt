package com.ultrapuremusic.service

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class MediaSessionCallback : MediaSession.Callback {

    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<MutableList<MediaItem>> {
        val updatedItems = mediaItems.map { item ->
            item.buildUpon().setUri(item.requestMetadata.mediaUri).build()
        }.toMutableList()
        return Futures.immediateFuture(updatedItems)
    }
}
