package com.ultrapuremusic.core.network.api

import com.ultrapuremusic.core.network.dto.YoutubeSearchResponse
import com.ultrapuremusic.core.network.dto.YoutubeVideosResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YoutubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoCategoryId") categoryId: String = "10",
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String
    ): YoutubeSearchResponse

    @GET("videos")
    suspend fun getTrendingVideos(
        @Query("part") part: String = "snippet,contentDetails",
        @Query("chart") chart: String = "mostPopular",
        @Query("videoCategoryId") categoryId: String = "10",
        @Query("maxResults") maxResults: Int = 20,
        @Query("regionCode") regionCode: String = "VN",
        @Query("key") apiKey: String
    ): YoutubeVideosResponse
}
