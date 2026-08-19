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
     * 방금 저장한 그룹의 updatedAt 이다 — 목록 응답의 같은 필드는 created_at 이라 출처가 다르다.
     * 포맷은 목록 응답과 같은 모양이다(오프셋 없음, 벽시계는 Asia/Seoul) — 소비할 때는
     * `Instant::parse`가 아니라 `LocalDateTime::parse` + 시간대 부여로 읽어야 한다.
     */
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    /** 생성자에게 방금 배정된 칩 */
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
