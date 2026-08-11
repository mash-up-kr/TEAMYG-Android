package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    @SerialName("idToken")
    val idToken: String,
    @SerialName("nonce")
    val nonce: String,
)
