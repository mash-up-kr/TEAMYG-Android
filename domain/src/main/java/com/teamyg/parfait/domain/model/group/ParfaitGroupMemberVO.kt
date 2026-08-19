package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 이 목록은 탈퇴자를 빼고 오므로 [NametagChipType.DEFAULT] 는
     * 오지 않는다.
     *
     * **계약은 비널인데 여기가 널 허용인 것은 의도다** — 구버전 서버나 모르는 타입 문자열을 만나도
     * 화면이 통째로 실패하지 않도록 매퍼가 `null` 로 접는다.
     */
    val nametagChip: NametagChipType?,
)
