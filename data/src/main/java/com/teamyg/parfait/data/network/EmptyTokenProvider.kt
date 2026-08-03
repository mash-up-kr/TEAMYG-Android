package com.teamyg.parfait.data.network

/**
 * TODO 토큰 저장 연동 시 실제 구현으로 교체
 */
class EmptyTokenProvider : TokenProvider {
    override fun getToken(): String? = null
}
