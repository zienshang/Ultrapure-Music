package com.ultrapuremusic.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "playlist_song_cross_ref", primaryKeys = ["playlist_id", "song_id"])
data class PlaylistSongCrossRef(
    @ColumnInfo(name = "playlist_id") val playlistId: String,
    @ColumnInfo(name = "song_id") val songId: String,
    val position: Int = 0,
    @ColumnInfo(name = "added_at") val addedAt: Long = System.currentTimeMillis()
)

