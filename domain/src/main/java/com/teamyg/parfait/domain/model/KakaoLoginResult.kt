package com.teamyg.parfait.domain.model

sealed interface KakaoLoginResult {
    data class Success(val token: String) : KakaoLoginResult

    data class Cancel(val throwable: Throwable?) : KakaoLoginResult

    data class Failure(val throwable: Throwable?) : KakaoLoginResult
}
