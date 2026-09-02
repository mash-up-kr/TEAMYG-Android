package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JoinParfaitGroupRequest(
    @SerialName("inviteCode")
    val inviteCode: String,
)
