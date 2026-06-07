package com.ultrapuremusic.data.repository

import android.content.Context
import com.ultrapuremusic.core.database.dao.HistoryDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.HistoryEntity
import com.ultrapuremusic.core.database.entity.toEntity
import com.ultrapuremusic.core.database.entity.toModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.core.util.safeApiCall
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.data.remote.MusicRemoteDataSource
import com.ultrapuremusic.core.di.DownloadOkHttpClient
import com.ultrapuremusic.domain.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteDataSource: MusicRemoteDataSource,
    private val songDao: SongDao,
    private val historyDao: HistoryDao,
    @DownloadOkHttpClient private val downloadClient: OkHttpClient,  // no body-logging, no read timeout
) : MusicRepository {

    override fun getTrendingSongs(): Flow<ResultWrapper<List<Song>>> = flow {
        emit(ResultWrapper.Loading)
        emit(safeApiCall { remoteDataSource.getTrendingSongs() })
    }

    override fun getRecentlyPlayed(): Flow<List<Song>> = flow {
        // Try backend history first; fall back to local Room data if backend is unavailable
        val backendSongs = remoteDataSource.getRecentlyPlayed()
        if (backendSongs.isNotEmpty()) {
            emit(backendSongs)
        } else {
            emitAll(songDao.getAllSongs().map { entities -> entities.map { it.toModel() }.take(20) })
        }
    }

    override fun getDownloadedSongs(): Flow<List<Song>> =
        songDao.getDownloadedSongs().map { entities -> entities.map { it.toModel() } }

    override suspend fun getRecommendations(): ResultWrapper<List<Song>> =
        safeApiCall { remoteDataSource.getRecommendations() }

    override suspend fun getPopularTracks(): ResultWrapper<List<Song>> =
        safeApiCall { remoteDataSource.getPopularTracks() }

    override suspend fun getYoutubeAccountFeed(): ResultWrapper<List<Song>> =
        safeApiCall { remoteDataSource.getYoutubeAccountFeed() }

    override suspend fun getCommunityPlaylists(): ResultWrapper<List<com.ultrapuremusic.data.model.Playlist>> =
        safeApiCall { remoteDataSource.getCommunityPlaylists() }

    override suspend fun searchSongs(query: String): ResultWrapper<List<Song>> =
        safeApiCall { remoteDataSource.searchSongs(query) }

    override suspend fun getSongById(id: String): ResultWrapper<Song> {
        val local = songDao.getSongById(id)
        if (local != null) return ResultWrapper.Success(local.toModel())
        return safeApiCall { remoteDataSource.getSongById(id) }
    }

    override suspend fun getSongStreamUrl(youtubeId: String, quality: Int): ResultWrapper<String> =
        safeApiCall { remoteDataSource.getStreamUrl(youtubeId, quality = quality) }

    override suspend fun getRelatedSongs(videoId: String): ResultWrapper<List<Song>> =
        safeApiCall { remoteDataSource.getRelatedSongs(videoId) }

    override suspend fun getLyrics(
        trackName: String,
        artistName: String,
        durationSec: Int,
    ): com.ultrapuremusic.core.network.dto.LyricsResponse? =
        remoteDataSource.getLyrics(trackName, artistName, durationSec)

    override suspend fun markAsPlayed(songId: String) {
        historyDao.recordPlay(HistoryEntity(songId = songId))
    }

    override suspend fun downloadSong(
        song: Song,
        onProgress: ((Float) -> Unit)?,
    ): ResultWrapper<String> =
        withContext(Dispatchers.IO) {
            try {
                val videoId = song.youtubeId?.ifBlank { null } ?: song.id
                Timber.d("downloadSong: start videoId=%s title=%s", videoId, song.title)

                // 1. Resolve a downloadable stream URL — refuses to fall back to the
                //    YouTube watch URL because that serves HTML, not audio.
                val streamUrl = remoteDataSource.getDownloadableStreamUrl(videoId)
                    ?: return@withContext ResultWrapper.Error(
                        "Backend chưa sẵn sàng — không lấy được link tải. " +
                                "Hãy kiểm tra server backend đang chạy."
                    )
                Timber.d("downloadSong: streamUrl resolved (%d chars)", streamUrl.length)

                // 2. Determine file extension from URL path (m4a / webm / mp4)
                val ext = when {
                    streamUrl.contains(".m4a",  ignoreCase = true) -> "m4a"
                    streamUrl.contains(".webm", ignoreCase = true) -> "webm"
                    streamUrl.contains(".opus", ignoreCase = true) -> "opus"
                    else -> "mp4"
                }
                val downloadDir = File(context.filesDir, "downloads").also { it.mkdirs() }
                val destFile    = File(downloadDir, "${videoId}.$ext")

                // 3. Stream download — dedicated client (no body-logging, 120 s read timeout)
                val request  = Request.Builder().url(streamUrl).build()
                val response = downloadClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) {
                        return@withContext ResultWrapper.Error(
                            "HTTP ${resp.code} khi tải \"${song.title}\""
                        )
                    }

                    // Reject HTML responses — happens when stream URL has expired or
                    // backend mistakenly returned a watch page URL.
                    val contentType = resp.header("Content-Type").orEmpty()
                    if (contentType.startsWith("text/", ignoreCase = true) ||
                        contentType.contains("html", ignoreCase = true)
                    ) {
                        return@withContext ResultWrapper.Error(
                            "URL không phải file audio (Content-Type: $contentType). " +
                                    "Có thể stream URL đã hết hạn."
                        )
                    }

                    val body = resp.body ?: return@withContext ResultWrapper.Error("Empty response body")
                    val expectedBytes = body.contentLength().takeIf { it > 0 } ?: -1L

                    // ── Disk-space guard ─────────────────────────────────────
                    // Refuse to start the download if the device clearly can't fit
                    // the file (or, when size is unknown, ensure a 50 MB safety
                    // floor). Catches OOM/disk-full BEFORE we write a single byte.
                    val availableBytes = downloadDir.usableSpace
                    val requiredBytes  = if (expectedBytes > 0)
                        expectedBytes + MIN_FREE_SPACE_BUFFER_BYTES
                    else
                        MIN_FREE_SPACE_BUFFER_BYTES
                    if (availableBytes < requiredBytes) {
                        val availableMb = availableBytes / 1_048_576L
                        val neededMb    = requiredBytes / 1_048_576L
                        Timber.w(
                            "downloadSong: insufficient storage — need %d MB, have %d MB free",
                            neededMb, availableMb,
                        )
                        return@withContext ResultWrapper.Error(
                            "Không đủ bộ nhớ để tải \"${song.title}\". " +
                                    "Cần ${neededMb} MB nhưng thiết bị chỉ còn ${availableMb} MB trống. " +
                                    "Hãy giải phóng dung lượng rồi thử lại."
                        )
                    }

                    body.byteStream().use { input ->
                        destFile.outputStream().use { output ->
                            val buffer       = ByteArray(8 * 1024)
                            var totalRead    = 0L
                            var lastReported = 0L
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                totalRead += read

                                // Throttle: emit at most every ~64 KB of progress
                                if (expectedBytes > 0 &&
                                    totalRead - lastReported >= PROGRESS_REPORT_INTERVAL_BYTES
                                ) {
                                    lastReported = totalRead
                                    val fraction = (totalRead.toFloat() / expectedBytes)
                                        .coerceIn(0f, 1f)
                                    onProgress?.invoke(fraction)
                                }
                            }
                            // Final 100% emit so the UI snaps to a full circle
                            if (expectedBytes > 0) onProgress?.invoke(1f)
                            Timber.d(
                                "downloadSong: wrote %d bytes (expected %d) to %s",
                                totalRead, expectedBytes, destFile.absolutePath,
                            )
                        }
                    }
                }

                // 4. Persist to Room (insert-or-replace so metadata is up-to-date)
                songDao.insertSong(
                    song.toEntity().copy(
                        isDownloaded = true,
                        localPath    = destFile.absolutePath,
                    )
                )

                Timber.i("Downloaded \"%s\" → %s", song.title, destFile.absolutePath)
                ResultWrapper.Success(destFile.absolutePath)
            } catch (e: Exception) {
                Timber.e(e, "downloadSong failed for song %s", song.id)
                ResultWrapper.Error(e.message ?: "Download failed")
            }
        }

    override suspend fun deleteDownload(songId: String) {
        // Delete the physical file first so we don't leave orphan bytes on disk.
        // Falls through gracefully if the file is already gone.
        withContext(Dispatchers.IO) {
            try {
                val entity = songDao.getSongById(songId)
                entity?.localPath?.takeIf { it.isNotBlank() }?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        val deleted = file.delete()
                        Timber.d("deleteDownload: %s → %s", path, if (deleted) "removed" else "delete failed")
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "deleteDownload: failed to remove file for song %s", songId)
            }
        }
        songDao.updateDownloadStatus(songId, false, null)
    }

    companion object {
        /**
         * Safety floor — even when the audio file fits, never drive the device
         * below this much free space. Prevents the OS from running out of room
         * for app state, photo capture buffers, etc. mid-download.
         */
        private const val MIN_FREE_SPACE_BUFFER_BYTES = 50L * 1024 * 1024  // 50 MB

        /** Emit a progress callback at most once per this many bytes. */
        private const val PROGRESS_REPORT_INTERVAL_BYTES = 64L * 1024       // 64 KB
    }
}
