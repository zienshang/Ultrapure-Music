package com.ultrapuremusic.feature.splash

import androidx.lifecycle.viewModelScope
import com.ultrapuremusic.core.ui.BaseViewModel
import com.ultrapuremusic.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SplashUiState {
    data object Loading : SplashUiState()
    data object Done : SplashUiState()
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel<SplashUiState>(SplashUiState.Loading) {

    private val _destination = MutableSharedFlow<String>()
    val destination: SharedFlow<String> = _destination.asSharedFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        launch {
            delay(800) // minimum splash display time
            val isLoggedIn = authRepository.isLoggedIn().first()
            updateState { SplashUiState.Done }
            _destination.emit(if (isLoggedIn) "home" else "login")
        }
    }
}
