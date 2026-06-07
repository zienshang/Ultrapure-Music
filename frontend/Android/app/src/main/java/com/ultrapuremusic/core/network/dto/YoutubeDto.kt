package com.ultrapuremusic.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YoutubeSearchResponse(
    val items: List<YoutubeSearchItem> = emptyList()
)

@Serializable
data class YoutubeSearchItem(
    val id: YoutubeVideoId = YoutubeVideoId(),
    val snippet: YoutubeSnippet = YoutubeSnippet()
)

@Serializable
data class YoutubeVideoId(
    @SerialName("videoId") val videoId: String = ""
)

@Serializable
data class YoutubeSnippet(
    val title: String = "",
    val channelTitle: String = "",
    val thumbnails: YoutubeThumbnails = YoutubeThumbnails(),
    val description: String = ""
)

@Serializable
data class YoutubeThumbnails(
    val medium: YoutubeThumbnail = YoutubeThumbnail(),
    val high: YoutubeThumbnail = YoutubeThumbnail()
)

@Serializable
data class YoutubeThumbnail(
    val url: String = ""
)

@Serializable
data class YoutubeVideosResponse(
    val items: List<YoutubeVideoItem> = emptyList()
)

@Serializable
data class YoutubeVideoItem(
    val id: String = "",
    val snippet: YoutubeSnippet = YoutubeSnippet(),
    val contentDetails: YoutubeContentDetails = YoutubeContentDetails()
)

@Serializable
data class YoutubeContentDetails(
    val duration: String = ""
)
