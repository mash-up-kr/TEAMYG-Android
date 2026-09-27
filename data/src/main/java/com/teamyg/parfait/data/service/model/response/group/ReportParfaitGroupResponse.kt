package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 신고가 성공하면 서버가 같은 트랜잭션에서 신고자를 그룹에서 탈퇴시킨다. 이후 그 그룹의 상세·닉네임 변경·신고는
 * 403 GROUP_NOT_JOINED 다(`docs/api/parfait-group.md`).
 */
@Serializable
data class ReportParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("reportId")
    val reportId: Long,
)
