package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyParfaitGroupDetailResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("inviteCode")
    val inviteCode: String,
    @SerialName("members")
    val members: List<ParfaitGroupMemberResponse>,
)
