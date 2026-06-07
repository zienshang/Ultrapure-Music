package com.ultrapuremusic.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("song_count") val songCount: Int = 0,
    val songs: List<Song> = emptyList(),
    @SerialName("youtube_playlist_id") val youtubePlaylistId: String? = null,
    @SerialName("is_synced") val isSynced: Boolean = false,
    @SerialName("created_at") val createdAt: Long = System.currentTimeMillis(),
    @SerialName("updated_at") val updatedAt: Long = System.currentTimeMillis()
)
