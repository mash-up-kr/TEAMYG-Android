package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MyParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("recentImageUrl")
    val recentImageUrl: String? = null,
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 마지막 토퍼가 이미 그룹을 나갔으면 `"DEFAULT"` 가 온다 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
