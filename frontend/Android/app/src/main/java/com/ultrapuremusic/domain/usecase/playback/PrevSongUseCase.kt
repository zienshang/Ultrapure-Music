package com.ultrapuremusic.domain.usecase.playback

import com.ultrapuremusic.domain.repository.PlayerRepository
import javax.inject.Inject

class PrevSongUseCase @Inject constructor(
    private val playerRepository: PlayerRepository
) {
    operator fun invoke() = playerRepository.skipToPrev()
}
