package com.ultrapuremusic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "song_id") val songId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

