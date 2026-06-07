package com.ultrapuremusic.domain.usecase.auth

import android.content.Intent
import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.data.model.User
import com.ultrapuremusic.domain.repository.AuthRepository
import javax.inject.Inject

class LoginYoutubeUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    fun getSignInIntent(): Intent = authRepository.getSignInIntent()

    suspend operator fun invoke(data: Intent?): ResultWrapper<User> =
        authRepository.handleSignInResult(data)
}
