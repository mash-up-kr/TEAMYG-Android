package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.repository.member.MemberRepository
import javax.inject.Inject

class ChangeGlobalNicknameUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(nickname: GlobalNickname): Result<GlobalNickname> =
        memberRepository.changeGlobalNickname(nickname)
}
