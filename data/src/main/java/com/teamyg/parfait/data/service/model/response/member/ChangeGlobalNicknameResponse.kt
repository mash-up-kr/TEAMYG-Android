package com.teamyg.parfait.data.service.model.response.member

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeGlobalNicknameResponse(
    @SerialName("nickname")
    val nickname: String,
)
