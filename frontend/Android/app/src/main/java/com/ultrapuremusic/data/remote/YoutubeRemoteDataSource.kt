package com.ultrapuremusic.data.remote

import com.ultrapuremusic.data.model.Playlist
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeRemoteDataSource @Inject constructor() {

    // TODO: inject YouTube Data API v3 Retrofit service

    suspend fun getUserPlaylists(): List<Playlist> {
        // TODO: call YouTube Data API playlists.list endpoint
        return emptyList()
    }

    suspend fun syncPlaylist(youtubePlaylistId: String): Playlist {
        // TODO: fetch playlist items from YouTube and map to Playlist model
        throw NotImplementedError("syncPlaylist not implemented")
    }
}
