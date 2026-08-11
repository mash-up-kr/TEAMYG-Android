package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChangeMyParfaitGroupNicknameRequest(
    @SerialName("groupNickname")
    val groupNickname: String,
)
