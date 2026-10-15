package com.teamyg.parfait.data.service.model.response.group

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param memberId 회원 계정 id 다. 그룹 멤버십 id 가 아니다.
 * @param nameTagChip `TYPE1`~`TYPE12` 중 하나이고 같은 응답 안에서 겹치지 않는다. 탈퇴자는 목록에 없어
 *   `DEFAULT` 가 오지 않는다(`docs/api/parfait-group.md` Nametag-Chip 배정 규칙).
 */
@Serializable
data class ParfaitGroupMemberResponse(
    @SerialName("memberId")
    val memberId: Long,
    @SerialName("groupNickname")
    val groupNickname: String,
    @SerialName("nameTagChip")
    val nameTagChip: String? = null,
)
