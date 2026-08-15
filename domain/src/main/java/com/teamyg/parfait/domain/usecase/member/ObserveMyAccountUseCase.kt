package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMyAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    operator fun invoke(): Flow<MyAccountVO?> = memberRepository.myAccount
}
