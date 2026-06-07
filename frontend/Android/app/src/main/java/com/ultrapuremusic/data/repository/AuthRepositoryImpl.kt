package com.ultrapuremusic.data.repository

import android.content.Intent
import com.ultrapuremusic.core.auth.GoogleSignInManager
import com.ultrapuremusic.core.datastore.TokenManager
import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.User
import com.ultrapuremusic.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val googleSignInManager: GoogleSignInManager,
    private val userPreferences: UserPreferences,
    private val tokenManager: TokenManager
) : AuthRepository {

    override fun isLoggedIn(): Flow<Boolean> = userPreferences.isLoggedIn

    override fun getCurrentUser(): Flow<User?> = flow {
        // TODO: load user from DataStore / remote
        emit(null)
    }

    override fun getSignInIntent(): Intent = googleSignInManager.getSignInIntent()

    override suspend fun handleSignInResult(data: Intent?): ResultWrapper<User> {
        return try {
            val account = googleSignInManager.handleSignInResult(data)
                ?: return ResultWrapper.Error("Sign-in cancelled or failed")
            val user = User(
                id = account.id ?: "",
                name = account.displayName ?: "",
                email = account.email ?: "",
                avatarUrl = account.photoUrl?.toString(),
                youtubeToken = tokenManager.getYoutubeToken()
            )
            userPreferences.saveUser(user.id, user.name, user.email, user.avatarUrl)
            ResultWrapper.Success(user)
        } catch (e: Exception) {
            ResultWrapper.Error(e.message ?: "Sign-in failed", e)
        }
    }

    override suspend fun logout(): ResultWrapper<Unit> {
        return try {
            googleSignInManager.signOut()
            userPreferences.clearUser()
            tokenManager.clearTokens()
            ResultWrapper.Success(Unit)
        } catch (e: Exception) {
            ResultWrapper.Error(e.message ?: "Logout failed", e)
        }
    }

    override suspend fun refreshToken(): ResultWrapper<String> {
        // TODO: implement token refresh via backend API
        return ResultWrapper.Error("Token refresh not implemented")
    }
}
