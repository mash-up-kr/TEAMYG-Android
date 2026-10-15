package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param recentImageUrl 오늘 캔버스(03시 경계)의 마지막 토핑만 가리킨다. 오늘 캔버스가 비면 `null` 이고,
 *   그것이 토핑 0건을 뜻하지는 않는다(`docs/api/parfait-group.md`).
 * @param recentImageBorderType `NONE`·`SOLID`. recentImageUrl 과 같은 토핑의 값이라 recentImageUrl 이 `null` 이면
 *   `null`, 값이 있으면 비널이다.
 * @param recentImageBorderColor `SOLID` 일 때만 값이 보장된다. `NONE` 이어도 값이 남아 있을 수 있어
 *   recentImageBorderType 을 먼저 본다.
 * @param recentImageBorderWidth recentImageBorderColor 와 같은 조건이고, 범위는 서버가 검사하지 않는다.
 * @param recentImageUploadedAt 오프셋 없는 로컬 날짜시각이고 벽시계는 KST다. 날짜와 무관한 마지막 토핑
 *   시각이고, 토핑이 한 건도 없으면 그룹 생성 시각으로 대체된다.
 * @param lastPlacedByNameTagChip recentImageUploadedAt 과 같은 토핑을 올린 사람의 칩이라 오늘 캔버스가 비면
 *   recentImageUrl 과 다른 토핑을 가리킨다. 토핑이 없으면 그룹 생성자의 칩이고, 마지막 토퍼가 이미 그룹을
 *   나갔으면 `"DEFAULT"` 가 온다.
 */
@Serializable
data class MyParfaitGroupResponse(
    @SerialName("groupId")
    val groupId: Long,
    @SerialName("groupName")
    val groupName: String,
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
