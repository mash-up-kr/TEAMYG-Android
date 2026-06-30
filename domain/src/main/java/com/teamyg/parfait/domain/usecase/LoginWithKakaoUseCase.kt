package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.model.useCaseLogger
import com.teamyg.parfait.domain.repository.KakaoUserRepository
import javax.inject.Inject

class LoginWithKakaoUseCase
@Inject
constructor(
    private val kakaoUserRepository: KakaoUserRepository,
) {
    init {
        useCaseLogger.i { "LoginWithKakaoUseCase::init" }
    }

    suspend operator fun invoke(): KakaoLoginResult {
        val isAvailable: Boolean = kakaoUserRepository.isKakaoTalkLoginAvailable()
        useCaseLogger.d { "LoginWithKakaoUseCase - isAvailable: $isAvailable" }

        if (isAvailable) {
            return when (val result = kakaoUserRepository.loginWithKakaoTalk()) {
                is KakaoLoginResult.Success -> result
                is KakaoLoginResult.Cancel -> result
                is KakaoLoginResult.Failure -> kakaoUserRepository.loginWithKakaoAccount()
            }
        }

        return kakaoUserRepository.loginWithKakaoAccount()
    }
}
