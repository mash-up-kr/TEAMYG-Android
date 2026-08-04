package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

data class ParfaitGroupDetailVO(
    val groupId: GroupId,
    val groupNickname: GroupNickname,
    val inviteCode: InviteCode,
    val members: List<ParfaitGroupMemberVO>,
)
