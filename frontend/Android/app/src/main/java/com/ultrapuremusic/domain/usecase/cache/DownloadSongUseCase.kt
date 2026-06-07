package com.ultrapuremusic.domain.usecase.cache

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import javax.inject.Inject

class DownloadSongUseCase @Inject constructor(
    private val musicRepository: MusicRepository,
) {
    /**
     * @param onProgress  Optional fraction (0f..1f) reported as more bytes arrive.
     *                    See [MusicRepository.downloadSong] for emission cadence.
     */
    suspend operator fun invoke(
        song: Song,
        onProgress: ((Float) -> Unit)? = null,
    ): ResultWrapper<String> = musicRepository.downloadSong(song, onProgress)
}
