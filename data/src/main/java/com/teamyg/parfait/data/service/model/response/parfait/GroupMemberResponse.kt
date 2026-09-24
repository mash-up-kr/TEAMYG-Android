package com.teamyg.parfait.data.service.model.response.parfait

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param id 계정 id 가 아니라 그룹 멤버십 행 id 다.
 * @param nameTagChip 서버가 그 그룹 안에서 배정한 칩. 이 목록은 탈퇴자를 빼고 오므로 `"DEFAULT"` 는
 *  오지 않는다.
 */
@Serializable
data class GroupMemberResponse(
    @SerialName("id")
    val id: Long,
    @SerialName("nickname")
    val nickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
