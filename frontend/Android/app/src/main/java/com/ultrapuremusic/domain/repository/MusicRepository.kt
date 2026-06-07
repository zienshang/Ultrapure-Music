package com.ultrapuremusic.domain.repository

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Playlist
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    fun getTrendingSongs(): Flow<ResultWrapper<List<Song>>>
    fun getRecentlyPlayed(): Flow<List<Song>>
    suspend fun getRecommendations(): ResultWrapper<List<Song>>
    suspend fun getPopularTracks(): ResultWrapper<List<Song>>
    suspend fun getYoutubeAccountFeed(): ResultWrapper<List<Song>>
    /** Public YouTube playlists made by other users ('Danh sách từ mọi người tạo'). */
    suspend fun getCommunityPlaylists(): ResultWrapper<List<Playlist>>
    fun getDownloadedSongs(): Flow<List<Song>>
    suspend fun searchSongs(query: String): ResultWrapper<List<Song>>
    suspend fun getSongById(id: String): ResultWrapper<Song>
    suspend fun getSongStreamUrl(youtubeId: String, quality: Int = 0): ResultWrapper<String>
    suspend fun getRelatedSongs(videoId: String): ResultWrapper<List<Song>>
    /** Returns synced + plain lyrics, or null when not found. */
    suspend fun getLyrics(trackName: String, artistName: String, durationSec: Int): com.ultrapuremusic.core.network.dto.LyricsResponse?
    suspend fun markAsPlayed(songId: String)
    /**
     * Download [song] for offline playback.
     * @param onProgress  Optional callback invoked with the fraction (0f..1f) every
     *                    time roughly 64 KB more bytes have been written. Only
     *                    fired when the server reports Content-Length; otherwise
     *                    progress is unknown and the callback is never called.
     */
    suspend fun downloadSong(
        song: Song,
        onProgress: ((Float) -> Unit)? = null,
    ): ResultWrapper<String>

    suspend fun deleteDownload(songId: String)
}
