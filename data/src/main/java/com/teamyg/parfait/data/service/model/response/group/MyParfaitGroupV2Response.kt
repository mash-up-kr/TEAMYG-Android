package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 필드마다 뜻은 `docs/api/parfait-group.md` GET /api/v2/parfait-groups 가 정본이다.
 *
 * @param recentImageUrl `null` 만으로 "오늘 토핑 없음"을 판정하지 않는다 — 이미지·텍스트는 [recentToppingType] 으로 가른다.
 * @param recentImageUploadedAt 오프셋 없는 로컬 날짜시각(벽시계 KST)이고, 이름과 달리 텍스트 토핑 시각도 담는다.
 */
@Serializable
data class MyParfaitGroupV2Response(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
    @SerialName("recentToppingType")
    val recentToppingType: String? = null,
    @SerialName("recentImageUrl")
    val recentImageUrl: String? = null,
    @SerialName("recentImageBorderType")
    val recentImageBorderType: String? = null,
    @SerialName("recentImageBorderColor")
    val recentImageBorderColor: String? = null,
    @SerialName("recentImageBorderWidth")
    val recentImageBorderWidth: Double? = null,
    @SerialName("recentImageUploadedAt")
    val recentImageUploadedAt: String? = null,
    @SerialName("lastPlacedByNameTagChip")
    val lastPlacedByNameTagChip: String? = null,
)
