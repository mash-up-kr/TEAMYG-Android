package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import javax.inject.Inject

/**
 * 회원 탈퇴. 서버에서 계정이 지워진 **뒤에야** 이 기기를 정리한다.
 *
 * 로그아웃과 순서가 반대다. 로그아웃은 서버 호출이 실패해도 로컬을 지우지만, 탈퇴는 서버가
 * 받아 주지 않으면 계정이 그대로 살아 있다 — 그때 로컬만 지우면 사용자는 탈퇴했다고 믿는데
 * 계정은 남는다. 그래서 실패를 그대로 올리고 아무것도 지우지 않는다.
 *
 * 지우는 일을 [LogoutUseCase] 에 맡기는 이유는 **"무엇을 지우는가"가 거기 한 곳**이기
 * 때문이다. 탈퇴한 계정으로 서버 로그아웃이 한 번 더 나가 실패하지만, 그 실패는 로컬 정리를
 * 막지 않는 것이 [LogoutUseCase] 의 계약이라 결과는 같다.
 */
class WithdrawUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val logout: LogoutUseCase,
) {
    suspend operator fun invoke(): Result<Unit> {
        val withdrawn = memberRepository.withdraw()
        if (withdrawn.isFailure.not()) {
            // 성공했으면 logout 처리도 하기
            logout()
        }

        return withdrawn
    }
}
