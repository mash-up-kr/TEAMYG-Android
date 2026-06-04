package com.teamyg.domain.usecase

import com.teamyg.domain.entity.KakaoLoginResult
import com.teamyg.domain.repository.KakaoUserRepository
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
