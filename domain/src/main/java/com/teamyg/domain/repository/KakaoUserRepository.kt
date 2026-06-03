package com.teamyg.domain.repository

import com.teamyg.domain.entity.KakaoLoginResult

interface KakaoUserRepository {
    fun isKakaoTalkLoginAvailable(): Boolean
    suspend fun loginWithKakaoTalk(): KakaoLoginResult
    suspend fun loginWithKakaoAccount(): KakaoLoginResult
}
