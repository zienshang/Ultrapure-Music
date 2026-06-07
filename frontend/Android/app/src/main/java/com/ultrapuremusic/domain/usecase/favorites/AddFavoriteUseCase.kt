package com.ultrapuremusic.domain.usecase.favorites

import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.FavoriteEntity
import com.ultrapuremusic.core.database.entity.toEntity
import com.ultrapuremusic.data.model.Song
import javax.inject.Inject

class AddFavoriteUseCase @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val songDao: SongDao,
) {
    /**
     * Mark [song] as a favourite.
     *
     * The song is also upserted into the `songs` table so that
     * [FavoritesViewModel] — which joins `favorites` with `songs` — can
     * always find and display it, even when the song has never been
     * downloaded or added to a local playlist.
     */
    suspend operator fun invoke(song: Song) {
        songDao.insertSong(song.toEntity())                        // upsert → songs table
        favoriteDao.addFavorite(FavoriteEntity(songId = song.id)) // insert → favorites table
    }
}
