package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [MyParfaitGroupResponse] 와 키가 같고 [recentToppingType] 이 더해진다. 이미지와 텍스트를 한 토핑 축으로 세서
 * 필드 뜻이 v1 과 갈린다(`docs/api/parfait-group.md` GET /api/v2/parfait-groups).
 *
 * @param recentToppingType 오늘 캔버스(03시 경계)의 마지막 토핑 종류 `IMAGE`·`TEXT`. 오늘 토핑이 없으면 `null`.
 * @param recentImageUrl 오늘 마지막 토핑이 이미지일 때만 값이 있다. 텍스트면 오늘 이미지가 있어도 `null` 이라
 *   [recentToppingType] 으로 갈라 읽는다. 테두리 세 필드도 같은 토핑을 가리킨다.
 * @param recentImageUploadedAt 오프셋 없는 로컬 날짜시각이고 벽시계는 KST다. 이름과 달리 텍스트를 포함한 마지막
 *   토핑 시각이고, 토핑이 한 건도 없으면 그룹 생성 시각으로 대체된다.
 * @param lastPlacedByNameTagChip recentImageUploadedAt 과 같은 토핑을 올린 사람의 칩이다.
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
