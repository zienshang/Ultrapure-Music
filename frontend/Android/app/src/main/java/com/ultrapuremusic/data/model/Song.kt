package com.ultrapuremusic.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("stream_url") val streamUrl: String? = null,
    @SerialName("youtube_id") val youtubeId: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_downloaded") val isDownloaded: Boolean = false,
    @SerialName("play_count") val playCount: Int = 0,
    @SerialName("added_at") val addedAt: Long = System.currentTimeMillis()
)
