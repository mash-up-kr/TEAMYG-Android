package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.UserInfoEntity
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO

internal fun MyAccountVO.toEntity(): UserInfoEntity = UserInfoEntity(
    memberId = memberId.value,
    provider = provider.name,
    nickname = nickname.value,
)

/** 저장 당시와 앱의 [LoginProvider] 목록이 다를 수 있어 알 수 없는 값은 UNKNOWN 으로 떨어뜨린다 */
internal fun UserInfoEntity.toVO(): MyAccountVO = MyAccountVO(
    memberId = MemberId(memberId),
    provider = LoginProvider.entries.firstOrNull { it.name == provider } ?: LoginProvider.UNKNOWN,
    nickname = GlobalNickname(nickname),
)
