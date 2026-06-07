package com.ultrapuremusic.domain.usecase.cache

import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOfflineSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(): Flow<List<Song>> = musicRepository.getDownloadedSongs()
}
