package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import javax.inject.Inject

/**
 * 사용자가 직접 로그아웃할 때 토큰과 계정 정보를 함께 정리한다.
 *
 * [AuthRepository.logout] 은 서버 호출이 실패해도 로컬 토큰을 지우고 항상
 * `Result.success` 를 돌려준다(계약) — 그래서 계정 정보 정리는 그 결과를 보지 않고
 * 항상 실행한다. 하나만 지우면 계정 전환 시 이전 사용자 정보가 남는다.
 *
 * [MemberRepository.clearMyAccount] 는 suspend 라 [runSuspendCatching] 으로 감싼다 —
 * 로컬 저장소 IO 가 실패해도 로그아웃 자체를 실패로 만들지 않는다(취소는 재던진다).
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = authRepository.logout()
        runSuspendCatching { memberRepository.clearMyAccount() }
        return result
    }
}
