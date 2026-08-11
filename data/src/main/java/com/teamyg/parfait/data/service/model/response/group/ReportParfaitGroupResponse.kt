package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("reportId")
    val reportId: Long,
)
