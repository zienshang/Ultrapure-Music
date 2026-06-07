package com.ultrapuremusic.data.remote

import com.ultrapuremusic.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRemoteDataSource @Inject constructor(
    private val musicRemoteDataSource: MusicRemoteDataSource
) {
    suspend fun search(query: String): List<Song> =
        musicRemoteDataSource.searchSongs(query)
}
