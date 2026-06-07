package com.ultrapuremusic.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Track ─────────────────────────────────────────────

@Serializable
data class BackendTrackResponse(
    val id: Int? = null,
    @SerialName("youtube_id") val youtubeId: String = "",
    val title: String = "",
    val artist: String = "",
    /** Duration in seconds (backend stores seconds, app uses ms) */
    val duration: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("view_count") val viewCount: Int = 0,
)

@Serializable
data class BackendTrackDetailResponse(
    val id: Int? = null,
    @SerialName("youtube_id") val youtubeId: String = "",
    val title: String = "",
    val artist: String = "",
    /** Duration in seconds */
    val duration: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("webpage_url") val webpageUrl: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String? = null,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    @SerialName("stream_url") val streamUrl: String? = null,
)

// ───── Search ─────────────────────────────────────────────

@Serializable
data class BackendSearchResponse(
    val query: String = "",
    val results: List<BackendTrackResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 10,
)

// ───── Health ─────────────────────────────────────────────

@Serializable
data class BackendHealthResponse(
    val status: String = "",
    val version: String = "",
    val database: String = "",
)

// ───── Favorites ──────────────────────────────────────────

@Serializable
data class FavoriteToggleResponse(
    val favorited: Boolean = false,
    @SerialName("track_id") val trackId: Int = 0,
)

@Serializable
data class FavoriteStatusResponse(
    @SerialName("youtube_id") val youtubeId: String = "",
    @SerialName("is_favorited") val isFavorited: Boolean = false,
)

@Serializable
data class FavoriteIdsResponse(
    @SerialName("favorite_ids") val favoriteIds: List<String> = emptyList(),
)

// ───── History ────────────────────────────────────────────

@Serializable
data class BackendHistoryResponse(
    val entries: List<BackendTrackResponse> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

// ───── Playlist ───────────────────────────────────────────

@Serializable
data class BackendPlaylistResponse(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    val name: String = "",
    val description: String = "",
    @SerialName("youtube_playlist_id") val youtubePlaylistId: String? = null,
    @SerialName("is_synced") val isSynced: Boolean = false,
    @SerialName("track_count") val trackCount: Int = 0,
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

// ───── Public YouTube Playlist (community-made) ──────────

@Serializable
data class YouTubePublicPlaylistResponse(
    @SerialName("youtube_playlist_id") val youtubePlaylistId: String = "",
    val name: String = "",
    val channel: String = "",
    @SerialName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("track_count") val trackCount: Int = 0,
)

// ───── YouTube Sync ───────────────────────────────────────

// ───── Playlist import ────────────────────────────────────

@Serializable
data class ImportPlaylistRequest(
    val name: String,
    @SerialName("tracks_text") val tracksText: String,
    @SerialName("user_id") val userId: Int = 1,
)

// ───── Lyrics ─────────────────────────────────────────────
@Serializable
data class LyricsResponse(
    @SerialName("track_name")    val trackName: String = "",
    @SerialName("artist_name")   val artistName: String = "",
    @SerialName("synced_lyrics") val syncedLyrics: String? = null,
    @SerialName("plain_lyrics")  val plainLyrics: String? = null,
)

@Serializable
data class YouTubeSyncStatsResponse(
    val status: String = "",
    val synced: Int = 0,
    val skipped: Int = 0,
    @SerialName("tracks_added") val tracksAdded: Int = 0,
    @SerialName("tracks_removed") val tracksRemoved: Int = 0,
)

@Serializable
data class BackendPlaylistDetailResponse(
    val id: Int = 0,
    @SerialName("user_id") val userId: Int = 0,
    val name: String = "",
    val description: String = "",
    @SerialName("youtube_playlist_id") val youtubePlaylistId: String? = null,
    @SerialName("is_synced") val isSynced: Boolean = false,
    @SerialName("track_count") val trackCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val tracks: List<BackendTrackResponse> = emptyList(),
)
