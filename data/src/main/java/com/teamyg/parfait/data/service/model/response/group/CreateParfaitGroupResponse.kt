package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("inviteCode")
    val inviteCode: String,
    @SerialName("memberLimit")
    val memberLimit: Int,
    /** 갓 만든 그룹이라 서버가 항상 `null` 을 넣는다 */
    @SerialName("recentImageUrl")
    val recentImageUrl: String? = null,
    /** 방금 저장한 그룹의 updatedAt 이다 — 목록 응답의 같은 필드는 created_at 이라 출처가 다르다 */
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 생성자에게 방금 배정된 칩 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
