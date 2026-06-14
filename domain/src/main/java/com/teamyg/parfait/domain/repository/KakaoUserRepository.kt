package com.teamyg.parfait.domain.repository

import com.teamyg.parfait.domain.model.KakaoLoginResult

interface KakaoUserRepository {
    fun isKakaoTalkLoginAvailable(): Boolean

    suspend fun loginWithKakaoTalk(): KakaoLoginResult

    suspend fun loginWithKakaoAccount(): KakaoLoginResult
}
