package com.ultrapuremusic.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String,
    val name: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("song_count") val songCount: Int = 0,
    @SerialName("youtube_channel_id") val youtubeChannelId: String? = null
)
