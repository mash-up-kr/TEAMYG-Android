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
    /**
     * 오프셋 없는 로컬 날짜시각이고 벽시계는 KST다 — `Instant::parse` 로 읽으면 던진다.
     * 목록 응답의 같은 이름 필드와 출처가 달라 두 값이 같다고 가정하면 안 된다.
     */
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 생성자에게 방금 배정된 칩 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
