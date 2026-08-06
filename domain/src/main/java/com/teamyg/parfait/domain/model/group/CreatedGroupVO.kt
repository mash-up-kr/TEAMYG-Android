package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

data class CreatedGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
    val inviteCode: InviteCode,
    val memberLimit: Int,
)
