package com.ultrapuremusic.core.auth

import com.ultrapuremusic.core.datastore.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YoutubeAuthManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    fun getAuthHeader(): String? {
        val token = tokenManager.getYoutubeToken() ?: return null
        if (!tokenManager.isYoutubeTokenValid()) return null
        return "Bearer $token"
    }

    fun saveToken(token: String, expiresInSeconds: Long) {
        val expiryMs = System.currentTimeMillis() + expiresInSeconds * 1000
        tokenManager.saveYoutubeToken(token, expiryMs)
    }

    fun isAuthenticated(): Boolean =
        tokenManager.getYoutubeToken() != null && tokenManager.isYoutubeTokenValid()

    fun clearAuth() = tokenManager.clearTokens()
}
