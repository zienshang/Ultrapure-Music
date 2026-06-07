package com.ultrapuremusic.core.auth

import com.ultrapuremusic.core.datastore.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenProvider @Inject constructor(
    private val tokenManager: TokenManager
) {
    fun getAccessToken(): String? = tokenManager.getAccessToken()

    fun getAuthorizationHeader(): String? {
        val token = tokenManager.getAccessToken() ?: return null
        return "Bearer $token"
    }

    fun hasValidToken(): Boolean = tokenManager.getAccessToken() != null
}
