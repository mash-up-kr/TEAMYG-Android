package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param refreshToken Authorization 헤더의 access token 과 같은 회원의 토큰이어야 한다. 다른 회원 것이면
 *   403 FORBIDDEN_REFRESH_TOKEN, 검증 실패는 401 INVALID_TOKEN·EXPIRED_TOKEN 이다.
 */
@Serializable
data class LogoutRequest(
    @SerialName("refreshToken")
    val refreshToken: String,
)
