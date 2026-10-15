package com.teamyg.parfait.data.service.model.response.parfaitimage

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 배치자. 캔버스 응답의 `PlacedByResponse` 와 이름이 다른 것은 서버 이름을 따랐기 때문이다.
 * 통일하거나 이름을 바꾸지 않는다(`docs/api/parfait-image.md`).
 *
 * @param groupMemberId 회원 id 가 아니라 그룹 멤버십 행 id 다. 토핑 소유권 판정이 이 값으로 한다.
 * @param nickname 전역 닉네임이 아니라 그룹 닉네임이다.
 * @param nameTagChip 읽는 화면이 생길 때 도메인으로 올린다.
 */
@Serializable
data class PlaceParfaitImagePlacedByResponse(
    @SerialName("groupMemberId")
    val groupMemberId: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
