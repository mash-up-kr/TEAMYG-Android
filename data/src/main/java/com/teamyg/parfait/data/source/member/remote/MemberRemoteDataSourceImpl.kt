package com.teamyg.parfait.data.source.member.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.MemberService
import com.teamyg.parfait.data.service.model.request.member.ChangeGlobalNicknameRequest
import com.teamyg.parfait.data.source.member.mapper.toGlobalNickname
import com.teamyg.parfait.data.source.member.mapper.toMyAccountVO
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.model.member.MyAccountVO
import javax.inject.Inject

class MemberRemoteDataSourceImpl @Inject constructor(
    private val memberService: MemberService,
    private val apiCaller: ApiCaller,
) : MemberRemoteDataSource {
    override suspend fun getMyAccount(): Result<MyAccountVO> = apiCaller.safeApiCall(
        block = { memberService.getUsersMe() },
        transform = { it.toMyAccountVO() },
    )

    override suspend fun changeGlobalNickname(nickname: GlobalNickname): Result<GlobalNickname> = apiCaller.safeApiCall(
        block = {
            memberService.patchUsersMeNickname(
                ChangeGlobalNicknameRequest(nickname = nickname.value),
            )
        },
        transform = { it.toGlobalNickname() },
    )
}
