package com.ultrapuremusic.data.remote

import com.ultrapuremusic.core.datastore.TokenManager
import com.ultrapuremusic.core.network.api.BackendApiService
import com.ultrapuremusic.core.network.api.YoutubeApiService
import com.ultrapuremusic.core.network.dto.BackendTrackResponse
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.core.network.dto.YoutubeSearchItem
import com.ultrapuremusic.core.network.dto.YoutubeVideoItem
import com.ultrapuremusic.core.util.Constants
import com.ultrapuremusic.data.model.Song
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRemoteDataSource @Inject constructor(
    private val youtubeApiService: YoutubeApiService,
    private val backendApiService: BackendApiService,
    private val tokenManager: TokenManager,
) {

    // ── Trending ─────────────────────────────────────────────────────────────

    /**
     * Trending feed. Uses the backend (yt-dlp, no API key) first; falls back to the
     * YouTube Data API only if the backend is unreachable.
     */
    suspend fun getTrendingSongs(): List<Song> {
        return try {
            backendApiService.getTrending(limit = 20).results.map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Backend trending unavailable, falling back to YouTube API")
            val response = youtubeApiService.getTrendingVideos(
                apiKey = Constants.YOUTUBE_API_KEY
            )
            response.items.map { it.toSong() }
        }
    }

    // ── Search ───────────────────────────────────────────────────────────────

    /**
     * Search via the backend (yt-dlp) first; fall back to YouTube Data API if the
     * backend is unreachable (e.g. during development without a running server).
     */
    suspend fun searchSongs(query: String): List<Song> {
        return try {
            val response = backendApiService.searchTracks(query = query, pageSize = 20)
            response.results.map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Backend search unavailable, falling back to YouTube API")
            val response = youtubeApiService.searchVideos(
                query = query,
                apiKey = Constants.YOUTUBE_API_KEY
            )
            response.items.map { it.toSong() }
        }
    }

    // ── Song by ID ───────────────────────────────────────────────────────────

    suspend fun getSongById(id: String): Song {
        return try {
            val detail = backendApiService.getStreamInfo(videoId = id)
            Song(
                id = detail.youtubeId.ifEmpty { id },
                title = detail.title,
                artist = detail.artist,
                thumbnailUrl = detail.thumbnailUrl,
                streamUrl = detail.streamUrl,
                youtubeId = detail.youtubeId.ifEmpty { id },
                durationMs = detail.duration * 1000L,
                isFavorite = detail.isFavorited,
            )
        } catch (e: Exception) {
            Timber.e(e, "getSongById failed for id=%s", id)
            throw e
        }
    }

    // ── Stream URL ───────────────────────────────────────────────────────────

    /**
     * Fetches a real audio stream URL from the backend (resolved via yt-dlp).
     * Falls back to the YouTube watch URL if the backend is unavailable, so
     * ExoPlayer can still attempt playback via the YouTube iframe/player.
     */
    suspend fun getStreamUrl(youtubeId: String, quality: Int = 0): String {
        return try {
            val detail = backendApiService.getStreamInfo(
                videoId = youtubeId,
                quality = quality.takeIf { it != 0 },
            )
            detail.streamUrl ?: "https://www.youtube.com/watch?v=$youtubeId"
        } catch (e: Exception) {
            Timber.w(e, "Backend stream URL unavailable for %s, using watch URL fallback", youtubeId)
            "https://www.youtube.com/watch?v=$youtubeId"
        }
    }

    suspend fun getRelatedSongs(videoId: String, limit: Int = 10): List<Song> {
        return try {
            backendApiService.getRelatedSongs(videoId = videoId, limit = limit).map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Related songs unavailable for %s", videoId)
            emptyList()
        }
    }

    /** Fetch synced/plain lyrics from backend (proxied from LRCLib). */
    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationSec: Int,
    ): com.ultrapuremusic.core.network.dto.LyricsResponse? {
        return try {
            backendApiService.getLyrics(
                trackName  = trackName,
                artistName = artistName,
                duration   = durationSec,
            )
        } catch (e: Exception) {
            Timber.w(e, "Lyrics unavailable for '%s'", trackName)
            null
        }
    }

    /**
     * Get a directly-downloadable audio stream URL from the backend.
     *
     * Unlike [getStreamUrl], this does NOT fall back to the YouTube watch URL —
     * because that URL serves an HTML page, not audio, and OkHttp would either
     * hang on the keep-alive connection or produce a garbage file. Returns null
     * to signal "cannot download right now" so the caller can show a clear error.
     */
    suspend fun getDownloadableStreamUrl(youtubeId: String): String? {
        return try {
            val detail = backendApiService.getStreamInfo(videoId = youtubeId)
            detail.streamUrl?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.w(e, "Backend stream URL unavailable for download of %s", youtubeId)
            null
        }
    }

    // ── Recommendations ────────────────────────────────────────────────────────

    /** Personalized 'For you' feed from the backend (history + favorites based). */
    suspend fun getRecommendations(userId: Int = 1, limit: Int = 20): List<Song> {
        return try {
            backendApiService.getRecommendations(userId = userId, limit = limit)
                .map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Backend recommendations unavailable")
            emptyList()
        }
    }

    /** Community most-played tracks ('Thịnh hành với người nghe'). */
    suspend fun getPopularTracks(limit: Int = 20): List<Song> {
        return try {
            backendApiService.getPopular(limit = limit).map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Backend popular tracks unavailable")
            emptyList()
        }
    }

    /**
     * Feed from the user's YouTube account (subscriptions). Returns empty if the
     * user hasn't connected YouTube or the token has expired.
     */
    suspend fun getYoutubeAccountFeed(limit: Int = 20): List<Song> {
        if (!tokenManager.isYoutubeTokenValid()) return emptyList()
        val token = tokenManager.getYoutubeToken() ?: return emptyList()
        return try {
            backendApiService.getYoutubeRecommendations(accessToken = token, limit = limit)
                .map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "YouTube account feed unavailable")
            emptyList()
        }
    }

    /**
     * Public playlists made by other YouTube users
     * ('Danh sách từ mọi người tạo'). Empty if YouTube isn't connected.
     */
    suspend fun getCommunityPlaylists(
        query: String = "nhạc hay playlist",
        limit: Int = 15,
    ): List<Playlist> {
        if (!tokenManager.isYoutubeTokenValid()) return emptyList()
        val token = tokenManager.getYoutubeToken() ?: return emptyList()
        return try {
            backendApiService.getYoutubePublicPlaylists(
                accessToken = token, query = query, limit = limit,
            ).map { dto ->
                Playlist(
                    id                = dto.youtubePlaylistId,  // use YT id directly
                    name              = dto.name,
                    description       = dto.channel,
                    thumbnailUrl      = dto.thumbnailUrl,
                    songCount         = dto.trackCount,
                    youtubePlaylistId = dto.youtubePlaylistId,
                    isSynced          = false,
                )
            }
        } catch (e: Exception) {
            Timber.w(e, "Community playlists unavailable")
            emptyList()
        }
    }

    /** Items of any public YouTube playlist (uses the YouTube OAuth token). */
    suspend fun getYoutubePlaylistItems(playlistId: String, limit: Int = 50): List<Song> {
        if (!tokenManager.isYoutubeTokenValid()) return emptyList()
        val token = tokenManager.getYoutubeToken() ?: return emptyList()
        return try {
            backendApiService.getYoutubePlaylistItems(
                playlistId = playlistId, accessToken = token, limit = limit,
            ).map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Public playlist items unavailable for %s", playlistId)
            emptyList()
        }
    }

    // ── History ──────────────────────────────────────────────────────────────

    /** Fetch recently played songs from the backend for the given user. */
    suspend fun getRecentlyPlayed(userId: Int = 1, limit: Int = 20): List<Song> {
        return try {
            backendApiService.getRecentTracks(userId = userId, limit = limit)
                .map { it.toSong() }
        } catch (e: Exception) {
            Timber.w(e, "Backend history unavailable")
            emptyList()
        }
    }

    // ── Mapping helpers ──────────────────────────────────────────────────────

    private fun YoutubeVideoItem.toSong() = Song(
        id = id,
        title = snippet.title,
        artist = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.high.url.ifEmpty { snippet.thumbnails.medium.url },
        youtubeId = id,
        durationMs = parseDuration(contentDetails.duration),
    )

    private fun YoutubeSearchItem.toSong() = Song(
        id = id.videoId,
        title = snippet.title,
        artist = snippet.channelTitle,
        thumbnailUrl = snippet.thumbnails.high.url.ifEmpty { snippet.thumbnails.medium.url },
        youtubeId = id.videoId,
        durationMs = 0L,
    )

    private fun BackendTrackResponse.toSong() = Song(
        id = youtubeId.ifEmpty { id?.toString() ?: "" },
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        youtubeId = youtubeId,
        durationMs = duration * 1000L,
    )

    /** Parse ISO 8601 duration: PT3M45S → milliseconds */
    private fun parseDuration(iso: String): Long {
        if (iso.isBlank()) return 0L
        val hours = Regex("(\\d+)H").find(iso)?.groupValues?.get(1)?.toLong() ?: 0L
        val minutes = Regex("(\\d+)M").find(iso)?.groupValues?.get(1)?.toLong() ?: 0L
        val seconds = Regex("(\\d+)S").find(iso)?.groupValues?.get(1)?.toLong() ?: 0L
        return (hours * 3600 + minutes * 60 + seconds) * 1000
    }
}
