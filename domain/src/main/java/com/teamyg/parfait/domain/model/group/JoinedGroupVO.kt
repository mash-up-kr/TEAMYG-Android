package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

data class JoinedGroupVO(
    val groupId: GroupId,
    val groupName: GroupName,
)
