package com.ultrapuremusic.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ultrapuremusic.core.database.converter.Converters
import com.ultrapuremusic.core.database.dao.FavoriteDao
import com.ultrapuremusic.core.database.dao.HistoryDao
import com.ultrapuremusic.core.database.dao.PlaylistDao
import com.ultrapuremusic.core.database.dao.SongDao
import com.ultrapuremusic.core.database.entity.FavoriteEntity
import com.ultrapuremusic.core.database.entity.HistoryEntity
import com.ultrapuremusic.core.database.entity.PlaylistEntity
import com.ultrapuremusic.core.database.entity.PlaylistSongCrossRef
import com.ultrapuremusic.core.database.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        FavoriteEntity::class,
        HistoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "ultrapure_music.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
