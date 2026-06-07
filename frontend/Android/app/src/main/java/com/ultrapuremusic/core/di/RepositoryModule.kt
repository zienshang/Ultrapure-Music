package com.ultrapuremusic.core.di

import com.ultrapuremusic.core.player.StreamUrlResolver
import com.ultrapuremusic.data.repository.AuthRepositoryImpl
import com.ultrapuremusic.data.repository.MusicRepositoryImpl
import com.ultrapuremusic.data.repository.PlaylistRepositoryImpl
import com.ultrapuremusic.data.repository.PlayerRepositoryImpl
import com.ultrapuremusic.data.repository.StreamUrlResolverImpl
import com.ultrapuremusic.domain.repository.AuthRepository
import com.ultrapuremusic.domain.repository.MusicRepository
import com.ultrapuremusic.domain.repository.PlaylistRepository
import com.ultrapuremusic.domain.repository.PlayerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindStreamUrlResolver(impl: StreamUrlResolverImpl): StreamUrlResolver
}
