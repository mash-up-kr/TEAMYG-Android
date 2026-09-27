package com.teamyg.parfait.data.service.model.request.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param reason 비었거나 공백뿐이면 400 INVALID_GROUP_REPORT_REASON 이다. 이 검사가 그룹·멤버십 확인보다
 *   먼저 돈다(`docs/api/parfait-group.md`).
 */
@Serializable
data class ReportParfaitGroupRequest(
    @SerialName("reason")
    val reason: String,
)
