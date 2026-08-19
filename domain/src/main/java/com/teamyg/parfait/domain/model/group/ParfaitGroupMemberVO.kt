package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 이 목록은 탈퇴자를 빼고 오므로 [NametagChipType.DEFAULT] 가
     * 제 뜻("반납된 자리")으로 오지는 않는다 — 다만 구버전 서버나 모르는 타입 문자열을 만나면
     * 매퍼가 그 값으로 접으므로 여기서 볼 수는 있다.
     */
    val nametagChip: NametagChipType,
)
