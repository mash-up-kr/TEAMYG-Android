package com.teamyg.parfait.data.service.model.request.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReissueRequest(
    @SerialName("refreshToken")
    val refreshToken: String,
)
