package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.domain.repository.auth.AuthRepository
import javax.inject.Inject

/**
 * 로그인이 필요한 목적지로 보내기 전에 세션이 있는지 묻는다. 판정 근거는
 * [BootstrapSessionUseCase] 가 자동로그인 여부를 가를 때와 같은
 * [AuthRepository.hasSession] 이다 — 두 자리가 서로 다른 근거를 쓰면 부트스트랩은
 * 로그인으로 보냈는데 이쪽은 통과시키는 어긋남이 생긴다.
 */
class HasActiveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Boolean = authRepository.hasSession()
}
