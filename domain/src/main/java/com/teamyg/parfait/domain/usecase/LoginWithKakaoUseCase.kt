package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.domain.model.KakaoLoginResult
import com.teamyg.parfait.domain.repository.KakaoUserRepository
import javax.inject.Inject

class LoginWithKakaoUseCase
@Inject
constructor(
    private val kakaoUserRepository: KakaoUserRepository,
) {
    suspend operator fun invoke(): KakaoLoginResult = if (kakaoUserRepository.isKakaoTalkLoginAvailable()) {
        when (val result = kakaoUserRepository.loginWithKakaoTalk()) {
            is KakaoLoginResult.Success -> result
            is KakaoLoginResult.Cancel -> result
            is KakaoLoginResult.Failure -> kakaoUserRepository.loginWithKakaoAccount()
        }
    } else {
        kakaoUserRepository.loginWithKakaoAccount()
    }
}
