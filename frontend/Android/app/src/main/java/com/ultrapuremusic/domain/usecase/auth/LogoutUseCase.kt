package com.ultrapuremusic.domain.usecase.auth

import com.ultrapuremusic.core.util.ResultWrapper
import com.ultrapuremusic.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): ResultWrapper<Unit> = authRepository.logout()
}
