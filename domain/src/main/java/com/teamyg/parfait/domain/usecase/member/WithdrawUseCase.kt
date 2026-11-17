package com.teamyg.parfait.domain.usecase.member

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.repository.member.MemberRepository
import com.teamyg.parfait.domain.repository.notice.NoticeRepository
import com.teamyg.parfait.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.flow.first
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
 *
 * 공지 읽음 기록만 예외로 여기서 먼저 지운다. 로그아웃은 같은 계정의 재로그인에 기록을
 * 이어 주려고 일부러 남기지만, 탈퇴한 계정은 돌아오지 않는다. [LogoutUseCase] 가 계정 정보를
 * 비우면 어느 계정의 기록인지 알 수 없으므로 그 앞에서 지운다. 지우기가 실패해도 탈퇴한
 * 계정이 로그인 상태로 남으면 안 되어 로그아웃은 이어 간다.
 */
class WithdrawUseCase @Inject constructor(
    private val memberRepository: MemberRepository,
    private val noticeRepository: NoticeRepository,
    private val logout: LogoutUseCase,
) {
    suspend operator fun invoke(): Result<Unit> {
        val withdrawn = memberRepository.withdraw()
        if (withdrawn.isFailure.not()) {
            runSuspendCatching {
                memberRepository.myAccount.first()?.memberId?.let { memberId ->
                    noticeRepository.clearReadNotices(memberId)
                }
            }
            logout()
        }

        return withdrawn
    }
}
