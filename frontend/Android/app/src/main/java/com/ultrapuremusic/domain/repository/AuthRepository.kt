package com.ultrapuremusic.domain.repository

import android.content.Intent
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    fun getSignInIntent(): Intent
    suspend fun handleSignInResult(data: Intent?): ResultWrapper<User>
    suspend fun logout(): ResultWrapper<Unit>
    suspend fun refreshToken(): ResultWrapper<String>
}
