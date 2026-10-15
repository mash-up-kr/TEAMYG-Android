package com.teamyg.parfait.data.service.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param isNewUser JSON 키가 `isNewUser` 그대로다. OpenAPI 스키마는 `newUser` 로 적지만 틀렸으니 따라 고치지
 *   않는다(`docs/api/auth.md`). true 면 registrationToken 만, false 면 accessToken·refreshToken·expiresIn 만
 *   값이 있고 나머지는 키가 null 로 실려 온다.
 * @param expiresIn access token 만료까지 남은 초(서버 기본 3600).
 */
@Serializable
data class KakaoLoginResponse(
    @SerialName("isNewUser")
    val isNewUser: Boolean,
    @SerialName("accessToken")
    val accessToken: String? = null,
    @SerialName("refreshToken")
    val refreshToken: String? = null,
    @SerialName("expiresIn")
    val expiresIn: Long? = null,
    @SerialName("registrationToken")
    val registrationToken: String? = null,
)
