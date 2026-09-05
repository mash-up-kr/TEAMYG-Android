package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 판정 근거를 [BootstrapSessionUseCase] 와 같은 [AuthRepository.hasSession] 하나로 묶는다 —
 * 근거가 갈리면 부트스트랩은 로그인으로 보냈는데 이쪽은 통과시키는 어긋남이 생긴다.
 */
class HasActiveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Boolean = authRepository.hasSession()
}
