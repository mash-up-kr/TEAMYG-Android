package com.teamyg.parfait.data.service.model.response.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param expiresIn access token 만료까지 남은 초(서버 기본 3600).
 */
@Serializable
data class SignupResponse(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
)
