package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateParfaitGroupRequest(
    @SerialName("groupName")
    val groupName: String,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("memberLimit")
    val memberLimit: Int,
)
