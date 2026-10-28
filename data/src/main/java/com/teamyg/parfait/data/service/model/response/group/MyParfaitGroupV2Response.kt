package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 필드마다 뜻은 `docs/api/parfait-group.md` GET /api/v2/parfait-groups 가 정본이다.
 *
 * @param recentImageUploadedAt 오프셋 없는 로컬 날짜시각(벽시계 KST)이다.
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
