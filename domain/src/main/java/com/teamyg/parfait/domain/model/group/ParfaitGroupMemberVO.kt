package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.MemberId

data class ParfaitGroupMemberVO(
    val memberId: MemberId,
    val groupNickname: GroupNickname,
    /**
     * 서버가 이 그룹 안에서 배정한 칩. 상세 응답은 탈퇴자를 빼고 주므로
     * [NametagChipType.DEFAULT] 는 오지 않는다.
     *
     * **서버 계약은 비널인데 여기가 널 허용인 것은 의도다** — 구버전 서버를 만나거나 앱이 모르는
     * 타입 문자열이 왔을 때 매퍼가 `null` 로 접어, 화면이 통째로 실패하는 대신 중립 색으로 그린다.
     */
    val nametagChip: NametagChipType?,
)
