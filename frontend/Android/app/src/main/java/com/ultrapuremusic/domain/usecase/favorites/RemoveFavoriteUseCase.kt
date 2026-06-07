package com.ultrapuremusic.domain.usecase.favorites

import com.ultrapuremusic.core.database.dao.FavoriteDao
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao
) {
    suspend operator fun invoke(songId: String) {
        favoriteDao.removeFavorite(songId)
    }
}
