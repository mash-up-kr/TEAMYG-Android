package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ParfaitGroupMemberResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("groupNickname")
    val groupNickname: String,
)
