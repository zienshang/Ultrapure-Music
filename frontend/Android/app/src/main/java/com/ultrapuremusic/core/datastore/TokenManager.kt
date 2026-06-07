package com.ultrapuremusic.core.datastore

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    private val encryptedPrefs: SharedPreferences
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_YOUTUBE_TOKEN = "youtube_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
    }

    fun saveAccessToken(token: String) {
        encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getAccessToken(): String? = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)

    fun saveRefreshToken(token: String) {
        encryptedPrefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)

    fun saveYoutubeToken(token: String, expiryMs: Long) {
        encryptedPrefs.edit()
            .putString(KEY_YOUTUBE_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, expiryMs)
            .apply()
    }

    fun getYoutubeToken(): String? = encryptedPrefs.getString(KEY_YOUTUBE_TOKEN, null)

    fun isYoutubeTokenValid(): Boolean {
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        return expiry > System.currentTimeMillis()
    }

    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_YOUTUBE_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }
}
