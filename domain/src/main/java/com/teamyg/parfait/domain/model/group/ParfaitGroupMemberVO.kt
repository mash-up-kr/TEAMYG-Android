package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 상세 응답은 탈퇴자를 빼고 주므로 실제로는
     * [NametagChipType.RELEASED] 도 `null` 도 오지 않지만, 계약 타입이 널 허용이라 그대로 받는다.
     */
    val nametagChip: NametagChipType?,
)
