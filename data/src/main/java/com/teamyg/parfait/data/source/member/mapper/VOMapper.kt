package com.teamyg.parfait.data.source.member.mapper

import com.teamyg.parfait.data.service.model.response.member.ChangeGlobalNicknameResponse
import com.teamyg.parfait.data.service.model.response.member.MyAccountResponse
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.LoginProvider
import com.teamyg.parfait.domain.model.member.MyAccountVO

internal fun MyAccountResponse.toMyAccountVO(): MyAccountVO = MyAccountVO(
    memberId = MemberId(memberId),
    provider = provider.toLoginProvider(),
    nickname = GlobalNickname(nickname),
)

internal fun ChangeGlobalNicknameResponse.toGlobalNickname(): GlobalNickname = GlobalNickname(nickname)

private fun String.toLoginProvider(): LoginProvider = when (this) {
    LoginProvider.KAKAO.name -> LoginProvider.KAKAO
    LoginProvider.APPLE.name -> LoginProvider.APPLE
    else -> LoginProvider.UNKNOWN
}
