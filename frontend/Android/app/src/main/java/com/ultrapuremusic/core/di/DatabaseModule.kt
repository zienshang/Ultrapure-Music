package com.ultrapuremusic.core.di

import android.content.Context
import com.ultrapuremusic.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.create(context)

    @Provides
    fun provideSongDao(db: AppDatabase) = db.songDao()

    @Provides
    fun providePlaylistDao(db: AppDatabase) = db.playlistDao()

    @Provides
    fun provideFavoriteDao(db: AppDatabase) = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase) = db.historyDao()
}
