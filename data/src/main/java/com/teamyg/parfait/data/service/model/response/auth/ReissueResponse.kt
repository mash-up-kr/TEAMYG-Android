package com.teamyg.parfait.data.service.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param refreshToken 회전된 새 토큰이다. 요청에 보낸 refresh token 은 서버에서 덮어써져 다시 쓰면
 *   401 INVALID_TOKEN 이므로 반드시 이 값으로 교체 저장한다.
 * @param expiresIn access token 만료까지 남은 초(서버 기본 3600).
 */
@Serializable
data class ReissueResponse(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
)
