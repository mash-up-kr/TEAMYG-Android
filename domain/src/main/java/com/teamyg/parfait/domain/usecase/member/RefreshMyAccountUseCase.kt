package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.model.member.MyAccountVO
import com.teamyg.parfait.domain.repository.member.MemberRepository
import javax.inject.Inject

class RefreshMyAccountUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(): Result<MyAccountVO> = memberRepository.refreshMyAccount()
}
