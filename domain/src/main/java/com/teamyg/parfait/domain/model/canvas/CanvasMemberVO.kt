package com.teamyg.parfait.domain.model.canvas

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.id.GroupMemberId

/**
 * 캔버스 응답이 함께 주는 그룹 멤버.
 *
 * 서버 응답의 id 는 계정(MemberId)이 아니라 그룹 멤버십 행(GroupMemberId)이다 —
 * 토핑의 placedBy.groupMemberId 와 같은 축이라 그 둘로 조인할 수 있다.
 * 다만 탈퇴한 멤버는 이 목록에서 빠지는데 그 토핑은 남으므로, 조인이 항상 성립하지는 않는다.
 */
data class CanvasMemberVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
    /** 서버가 배정한 칩. 앱이 모르는 값이면 `null` 이다. */
    val nametagChip: NametagChipType?,
)
