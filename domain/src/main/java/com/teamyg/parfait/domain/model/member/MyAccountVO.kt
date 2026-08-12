package com.teamyg.parfait.domain.model.member

import com.teamyg.parfait.domain.model.id.MemberId

data class MyAccountVO(
    val memberId: MemberId,
    val provider: LoginProvider,
    val nickname: GlobalNickname,
)
