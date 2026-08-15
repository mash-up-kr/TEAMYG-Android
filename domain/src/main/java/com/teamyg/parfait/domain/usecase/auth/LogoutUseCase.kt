package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
