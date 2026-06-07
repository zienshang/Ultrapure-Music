package com.ultrapuremusic.core.network.api

import com.ultrapuremusic.core.network.dto.BackendHealthResponse
import com.ultrapuremusic.core.network.dto.ImportPlaylistRequest
import com.ultrapuremusic.core.network.dto.LyricsResponse
import com.ultrapuremusic.core.network.dto.BackendHistoryResponse
import com.ultrapuremusic.core.network.dto.BackendPlaylistDetailResponse
import com.ultrapuremusic.core.network.dto.BackendPlaylistResponse
import com.ultrapuremusic.core.network.dto.BackendSearchResponse
import com.ultrapuremusic.core.network.dto.BackendTrackDetailResponse
import com.ultrapuremusic.core.network.dto.BackendTrackResponse
import com.ultrapuremusic.core.network.dto.FavoriteIdsResponse
import com.ultrapuremusic.core.network.dto.FavoriteStatusResponse
import com.ultrapuremusic.core.network.dto.FavoriteToggleResponse
import com.ultrapuremusic.core.network.dto.YouTubePublicPlaylistResponse
import com.ultrapuremusic.core.network.dto.YouTubeSyncStatsResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface BackendApiService {

    // ── Health ──────────────────────────────────────────────────

    @GET("api/v1/health")
    suspend fun health(): BackendHealthResponse

    // ── Player / Stream ─────────────────────────────────────────

    /**
     * Get real audio stream URL for a YouTube video (resolved via yt-dlp on the backend).
     * [userId] is optional; when provided, playback is logged to history.
     */
    @GET("api/v1/player/stream")
    suspend fun getStreamInfo(
        @Query("video_id") videoId: String,
        @Query("user_id") userId: Int? = null,
        @Query("quality") quality: Int? = null,
    ): BackendTrackDetailResponse

    @POST("api/v1/player/play")
    suspend fun logPlayback(
        @Query("video_id") videoId: String,
        @Query("user_id") userId: Int? = null,
        @Query("duration_played") durationPlayed: Int = 0,
    ): Map<String, String>

    // ── Search ──────────────────────────────────────────────────

    /**
     * Full-text search using yt-dlp on the backend.
     * Returns paginated results cached by the backend.
     */
    @GET("api/v1/search")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
    ): BackendSearchResponse

    /**
     * Trending feed sourced via yt-dlp on the backend (no YouTube Data API key).
     */
    @GET("api/v1/search/trending")
    suspend fun getTrending(
        @Query("limit") limit: Int = 20,
    ): BackendSearchResponse

    /**
     * 'For you' recommendations personalized from the user's history & favorites.
     */
    @GET("api/v1/recommendations")
    suspend fun getRecommendations(
        @Query("user_id") userId: Int = 1,
        @Query("limit") limit: Int = 20,
    ): List<BackendTrackResponse>

    /**
     * Feed built from the user's YouTube account (subscribed channels' uploads).
     */
    @GET("api/v1/recommendations/youtube")
    suspend fun getYoutubeRecommendations(
        @Query("access_token") accessToken: String,
        @Query("limit") limit: Int = 20,
    ): List<BackendTrackResponse>

    /**
     * Public playlists made by YouTube users ('Danh sách từ mọi người tạo').
     */
    @GET("api/v1/recommendations/youtube-playlists")
    suspend fun getYoutubePublicPlaylists(
        @Query("access_token") accessToken: String,
        @Query("q") query: String = "nhạc hay playlist",
        @Query("limit") limit: Int = 15,
    ): List<YouTubePublicPlaylistResponse>

    /**
     * Fetch items of any public YouTube playlist by id.
     */
    @GET("api/v1/recommendations/youtube-playlists/{playlist_id}/items")
    suspend fun getYoutubePlaylistItems(
        @Path("playlist_id") playlistId: String,
        @Query("access_token") accessToken: String,
        @Query("limit") limit: Int = 50,
    ): List<BackendTrackResponse>

    // ── Favorites ───────────────────────────────────────────────

    @GET("api/v1/favorites")
    suspend fun getFavorites(
        @Query("user_id") userId: Int = 1,
    ): List<BackendTrackResponse>

    @POST("api/v1/favorites")
    suspend fun toggleFavorite(
        @Query("youtube_id") youtubeId: String,
        @Query("user_id") userId: Int = 1,
    ): FavoriteToggleResponse

    @DELETE("api/v1/favorites")
    suspend fun removeFavorite(
        @Query("youtube_id") youtubeId: String,
        @Query("user_id") userId: Int = 1,
    ): Map<String, String>

    @GET("api/v1/favorites/status")
    suspend fun getFavoriteStatus(
        @Query("youtube_id") youtubeId: String,
        @Query("user_id") userId: Int = 1,
    ): FavoriteStatusResponse

    @GET("api/v1/favorites/ids")
    suspend fun getFavoriteIds(
        @Query("user_id") userId: Int = 1,
    ): FavoriteIdsResponse

    // ── History ─────────────────────────────────────────────────

    @GET("api/v1/history/recent")
    suspend fun getRecentTracks(
        @Query("user_id") userId: Int = 1,
        @Query("limit") limit: Int = 20,
    ): List<BackendTrackResponse>

    @GET("api/v1/history/most-played")
    suspend fun getMostPlayed(
        @Query("user_id") userId: Int = 1,
        @Query("limit") limit: Int = 20,
    ): List<Map<String, String>>

    /**
     * Community-wide most-played tracks ('Thịnh hành với người nghe').
     * Falls back to a popular feed on the backend when history is sparse.
     */
    @GET("api/v1/history/popular")
    suspend fun getPopular(
        @Query("limit") limit: Int = 20,
    ): List<BackendTrackResponse>

    @POST("api/v1/history")
    suspend fun recordPlayback(
        @Query("youtube_id") youtubeId: String,
        @Query("user_id") userId: Int = 1,
        @Query("duration_played") durationPlayed: Int = 0,
    ): Map<String, String>

    // ── Playlists ───────────────────────────────────────────────

    @GET("api/v1/playlists")
    suspend fun getPlaylists(
        @Query("user_id") userId: Int = 1,
    ): List<BackendPlaylistResponse>

    @GET("api/v1/playlists/{id}")
    suspend fun getPlaylist(
        @Path("id") playlistId: Int,
    ): BackendPlaylistDetailResponse

    /** Import playlist from a newline-separated list of "Artist - Title" entries. */
    @POST("api/v1/playlists/import/text")
    suspend fun importPlaylistFromText(@Body request: ImportPlaylistRequest): BackendPlaylistResponse

    /** Export a playlist as plain text ("Artist - Title" per line). */
    @GET("api/v1/playlists/{id}/export")
    suspend fun exportPlaylist(
        @Path("id") playlistId: Int,
        @Query("format") format: String = "text",
    ): okhttp3.ResponseBody

    @POST("api/v1/playlists/{id}/tracks")
    suspend fun addTrackToPlaylist(
        @Path("id") playlistId: Int,
        @Query("youtube_id") youtubeId: String,
    ): Map<String, String>

    @DELETE("api/v1/playlists/{id}/tracks")
    suspend fun removeTrackFromPlaylist(
        @Path("id") playlistId: Int,
        @Query("youtube_id") youtubeId: String,
    ): Map<String, String>

    // ── Lyrics ──────────────────────────────────────────────────

    /**
     * Lời bài hát từ LRCLib — trả về cả dạng synced (LRC) và plain text.
     */
    @GET("api/v1/lyrics")
    suspend fun getLyrics(
        @Query("track_name")  trackName: String,
        @Query("artist_name") artistName: String = "",
        @Query("duration")    duration: Int = 0,
    ): LyricsResponse

    /**
     * 'Bài hát tương tự' — tracks similar to the given video, found via yt-dlp search.
     */
    @GET("api/v1/recommendations/related")
    suspend fun getRelatedSongs(
        @Query("video_id") videoId: String,
        @Query("limit") limit: Int = 10,
    ): List<BackendTrackResponse>

    // ── YouTube Sync ─────────────────────────────────────────────

    /**
     * Sync ALL playlists from the user's YouTube account.
     * [accessToken] is the YouTube OAuth access token obtained via GoogleAuthUtil.
     */
    @POST("api/v1/playlists/youtube/sync-all")
    suspend fun syncAllYoutubePlaylists(
        @Query("access_token") accessToken: String,
        @Query("user_id") userId: Int = 1,
    ): YouTubeSyncStatsResponse

    /**
     * Sync a single YouTube playlist by its YouTube playlist ID.
     */
    @POST("api/v1/playlists/youtube/sync/{youtube_playlist_id}")
    suspend fun syncSingleYoutubePlaylist(
        @Path("youtube_playlist_id") youtubePlaylistId: String,
        @Query("access_token") accessToken: String,
        @Query("user_id") userId: Int = 1,
    ): YouTubeSyncStatsResponse
}
