package com.ultrapuremusic.core.util

object Constants {
    // Backend URL: 10.0.2.2 = host machine from Android emulator; change to real URL for production
    const val BASE_URL = "http://10.0.2.2:8000/"

    const val YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/"

    // Injected at compile time from local.properties → BuildConfig.
    // Add  youtubeApiKey=<your_key>  to local.properties (gitignored) and sync Gradle.
    val YOUTUBE_API_KEY: String get() = com.ultrapuremusic.BuildConfig.YOUTUBE_API_KEY

    // DataStore keys
    const val PREFS_NAME = "ultrapure_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_ACCESS_TOKEN = "access_token"
    const val KEY_THEME = "theme"

    // Player
    const val NOTIFICATION_ID = 1001
    const val NOTIFICATION_CHANNEL_ID = "ultrapure_playback"
    const val NOTIFICATION_CHANNEL_NAME = "Ultrapure Music Playback"

    // Cache
    const val CACHE_DIR = "ultrapure_cache"
    const val MAX_CACHE_SIZE = 500L * 1024 * 1024 // 500 MB

    // Pagination
    const val PAGE_SIZE = 20

    // Animation durations (ms)
    const val ANIM_DURATION_SHORT = 200
    const val ANIM_DURATION_NORMAL = 300
    const val ANIM_DURATION_LONG = 400

    // Networking
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
}
