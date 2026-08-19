package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. `null` 은 값이 없거나 앱이 모르는 값이라는 뜻이다 —
     * 상세 응답은 탈퇴자를 빼고 주므로 실제로는 [NametagChipType.RELEASED] 도 `null` 도
     * 잘 오지 않지만, 계약 타입이 널 허용인 데다 앱이 아직 모르는 타입 문자열이 오면
     * 매퍼가 그것도 `null` 로 접으므로 그대로 받는다.
     */
    val nametagChip: NametagChipType?,
)
