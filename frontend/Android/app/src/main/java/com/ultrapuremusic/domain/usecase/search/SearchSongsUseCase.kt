package com.ultrapuremusic.domain.usecase.search

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.Song
import com.ultrapuremusic.domain.repository.MusicRepository
import javax.inject.Inject

class SearchSongsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(query: String): ResultWrapper<List<Song>> {
        if (query.isBlank()) return ResultWrapper.Success(emptyList())
        return musicRepository.searchSongs(query.trim())
    }
}
