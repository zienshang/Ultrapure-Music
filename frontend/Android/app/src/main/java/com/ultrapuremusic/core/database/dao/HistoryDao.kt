package com.ultrapuremusic.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ultrapuremusic.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordPlay(history: HistoryEntity)

    @Query("SELECT DISTINCT song_id FROM history ORDER BY played_at DESC LIMIT :limit")
    fun getRecentSongIds(limit: Int = 50): Flow<List<String>>

    @Query("DELETE FROM history WHERE song_id = :songId")
    suspend fun clearHistoryForSong(songId: String)

    @Query("DELETE FROM history")
    suspend fun clearAllHistory()
}

