package com.ultrapuremusic.feature.profile

import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userEmail: String = "",
    val avatarUrl: String? = null,
    val isLoggingOut: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val logoutUseCase: LogoutUseCase,
) : BaseViewModel<ProfileUiState>(ProfileUiState()) {

    /** Emitted once logout (and token clearing) succeeds. */
    private val _logoutEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvent: SharedFlow<Unit> = _logoutEvent.asSharedFlow()

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        launch {
            val name   = userPreferences.userName.first()
            val email  = userPreferences.userEmail.first()
            val avatar = userPreferences.avatarUrl.first()
            updateState { copy(userName = name, userEmail = email, avatarUrl = avatar) }
        }
    }

    fun logout() {
        launch {
            updateState { copy(isLoggingOut = true, error = null) }
            when (val result = logoutUseCase()) {
                is ResultWrapper.Success -> {
                    updateState { copy(isLoggingOut = false) }
                    _logoutEvent.emit(Unit)
                }
                is ResultWrapper.Error -> updateState {
                    copy(isLoggingOut = false, error = "Đăng xuất thất bại: ${result.message}")
                }
                is ResultWrapper.Loading -> Unit
            }
        }
    }

    fun dismissError() = updateState { copy(error = null) }
}
