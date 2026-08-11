package com.teamyg.parfait.domain.model.group

import com.teamyg.parfait.domain.model.id.GroupId

data class GroupNicknameVO(
    val groupId: GroupId,
    val groupNickname: GroupNickname,
)
