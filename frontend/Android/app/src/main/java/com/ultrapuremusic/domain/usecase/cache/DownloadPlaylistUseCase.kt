package com.ultrapuremusic.domain.usecase.cache

import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject

data class PlaylistDownloadProgress(
    val downloaded: Int,   // newly downloaded this session
    val skipped: Int,      // already on disk — no re-download needed
    val failed: Int,       // errors (network, expired URL, etc.)
    val total: Int,
) {
    val processed: Int get() = downloaded + skipped + failed
    val isDone: Boolean get() = processed >= total
    val fraction: Float get() = if (total > 0) processed.toFloat() / total else 0f
}

/**
 * Downloads every song in [songs] sequentially.
 *
 * Songs already marked `isDownloaded` in Room are skipped so the user
 * doesn't re-download files they already have.
 *
 * Emits a [PlaylistDownloadProgress] snapshot after every song is processed
 * so the UI can show a live progress bar.
 */
class DownloadPlaylistUseCase @Inject constructor(
    private val downloadSongUseCase: DownloadSongUseCase,
    private val songDao: SongDao,
) {
    operator fun invoke(songs: List<Song>): Flow<PlaylistDownloadProgress> = flow {
        var downloaded = 0
        var skipped    = 0
        var failed     = 0
        val total      = songs.size

        Timber.i("DownloadPlaylistUseCase: starting %d songs", total)

        // Immediate "0/total" emit so the UI can flip into in-flight state right away
        // — otherwise the user would see no feedback until the first song finishes,
        // which can take 30+ seconds if the first download fails by timeout.
        emit(PlaylistDownloadProgress(0, 0, 0, total))

        songs.forEachIndexed { index, song ->
            Timber.d("DownloadPlaylistUseCase: [%d/%d] %s", index + 1, total, song.title)
            // Check Room DB — skip if the song file is already on disk
            val cached = songDao.getSongById(song.id)
            if (cached?.isDownloaded == true) {
                skipped++
            } else {
                val result = downloadSongUseCase(song)
                if (result is com.ultrapuremusic.core.util.ResultWrapper.Success) {
                    downloaded++
                } else {
                    failed++
                    val err = (result as? com.ultrapuremusic.core.util.ResultWrapper.Error)?.message
                    Timber.w("DownloadPlaylistUseCase: failed \"%s\" — %s", song.title, err)
                }
            }
            emit(PlaylistDownloadProgress(downloaded, skipped, failed, total))
        }

        Timber.i(
            "DownloadPlaylistUseCase: done — %d downloaded, %d skipped, %d failed",
            downloaded, skipped, failed,
        )
    }
}
