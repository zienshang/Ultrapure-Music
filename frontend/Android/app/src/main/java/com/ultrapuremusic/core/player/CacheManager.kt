package com.ultrapuremusic.core.player

import android.content.Context
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.ultrapuremusic.core.util.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val simpleCache: SimpleCache by lazy {
        val cacheDir = File(context.cacheDir, Constants.CACHE_DIR)
        val evictor = LeastRecentlyUsedCacheEvictor(Constants.MAX_CACHE_SIZE)
        SimpleCache(cacheDir, evictor)
    }

    fun release() {
        try {
            simpleCache.release()
        } catch (_: Exception) {}
    }
}
