package com.ultrapuremusic.feature.auth

import android.content.Intent
import com.google.android.gms.auth.UserRecoverableAuthException
import com.ultrapuremusic.core.auth.GoogleSignInManager
import com.ultrapuremusic.core.datastore.TokenManager
import com.ultrapuremusic.core.network.api.BackendApiService
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.User
import com.ultrapuremusic.domain.usecase.auth.LoginYoutubeUseCase
import com.ultrapuremusic.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data class Loading(val message: String) : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginYoutubeUseCase: LoginYoutubeUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val googleSignInManager: GoogleSignInManager,
    private val tokenManager: TokenManager,
    private val backendApiService: BackendApiService,
) : BaseViewModel<AuthUiState>(AuthUiState.Idle) {

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent.asSharedFlow()

    /**
     * Emitted when the user must complete an interactive grant to give the app
     * YouTube read access. The screen should launch this Intent, then call
     * [resumeAfterPermissionGrant] regardless of the result.
     */
    private val _youtubePermissionIntent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val youtubePermissionIntent: SharedFlow<Intent> = _youtubePermissionIntent.asSharedFlow()

    // Cached after a successful Google sign-in so we can resume the flow
    // (YouTube token + sync) after a permission grant round-trip.
    private var pendingUser: User? = null

    fun getSignInIntent(): Intent = loginYoutubeUseCase.getSignInIntent()

    fun handleSignInResult(data: Intent?) {
        launch {
            updateState { AuthUiState.Loading("Đang đăng nhập...") }
            when (val result = loginYoutubeUseCase(data)) {
                is ResultWrapper.Success -> {
                    pendingUser = result.data
                    proceedWithYoutubeSync()
                }
                is ResultWrapper.Error   -> updateState { AuthUiState.Error(result.message) }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    /** Called by the screen after the YouTube permission grant intent returns. */
    fun resumeAfterPermissionGrant() {
        launch { proceedWithYoutubeSync() }
    }

    /**
     * After Google sign-in: fetch the YouTube OAuth token, save it, and trigger
     * a backend playlist sync. Each step degrades gracefully — if any of them
     * fails the user still lands on Home.
     */
    private suspend fun proceedWithYoutubeSync() {
        val user = pendingUser ?: run {
            _navigationEvent.emit("home")
            return
        }

        // 1) YouTube token
        updateState { AuthUiState.Loading("Đang kết nối YouTube...") }
        val token = try {
            googleSignInManager.getYoutubeToken()
        } catch (e: UserRecoverableAuthException) {
            // First-time grant needed (rare — the scope is now part of sign-in,
            // but devices that already had a sign-in cache may miss it).
            val intent = e.intent
            if (intent != null) {
                _youtubePermissionIntent.emit(intent)
                // Stay in Loading state — resumeAfterPermissionGrant will re-enter.
                return
            }
            null
        } catch (e: Exception) {
            null
        }

        if (token.isNullOrBlank()) {
            // Skip sync silently; user is still signed in.
            finishLogin(user)
            return
        }

        // Save token (valid ~1 h)
        tokenManager.saveYoutubeToken(token, System.currentTimeMillis() + 3_600_000L)

        // 2) Auto-sync playlists
        updateState { AuthUiState.Loading("Đang đồng bộ playlist từ YouTube...") }
        try {
            backendApiService.syncAllYoutubePlaylists(accessToken = token)
        } catch (_: Exception) {
            // Sync failure shouldn't block login — the user can retry later
            // (e.g. by logging out and back in).
        }

        finishLogin(user)
    }

    private suspend fun finishLogin(user: User) {
        updateState { AuthUiState.Success(user) }
        _navigationEvent.emit("home")
    }

    fun logout() {
        launch {
            logoutUseCase()
            updateState { AuthUiState.Idle }
            _navigationEvent.emit("login")
        }
    }
}
