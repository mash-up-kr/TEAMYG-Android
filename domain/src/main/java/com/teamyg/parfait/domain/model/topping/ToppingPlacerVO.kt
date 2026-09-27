package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.id.GroupMemberId

/**
 * @param nickname 전역 닉네임이 아니라 그룹 안에서 쓰는 이름이다.
 */
data class ToppingPlacerVO(
    val groupMemberId: GroupMemberId,
    val nickname: GroupNickname,
)
