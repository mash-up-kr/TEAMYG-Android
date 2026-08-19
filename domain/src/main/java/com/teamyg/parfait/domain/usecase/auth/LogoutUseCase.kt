package com.teamyg.parfait.domain.usecase.auth

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import javax.inject.Inject

/**
 * 세션을 끝낼 때 토큰·계정 정보·그룹 캐시를 함께 정리한다 — **"무엇을 지우는가"의 단일 자리**다.
 *
 * 호출자마다 다른 것은 **언제 부르는가**뿐이다 — 사용자가 직접 로그아웃하는 S-001,
 * 자동로그인이 인증 거절로 실패했을 때의 `BootstrapSessionUseCase`, 서버가 계정을 지워 준
 * 뒤의 `WithdrawUseCase`. 지울 대상이 같으므로 한쪽만 늘어나지 않도록 여기 모은다.
 *
 * [AuthRepository.logout] 은 서버 호출이 실패해도 로컬 토큰을 지우고 항상
 * `Result.success` 를 돌려준다(계약) — 그래서 계정 정보 정리는 그 결과를 보지 않고
 * 항상 실행한다. 하나만 지우면 계정 전환 시 이전 사용자 정보가 남는다.
 *
 * [MemberRepository.clearMyAccount] 는 suspend 라 [runSuspendCatching] 으로 감싼다 —
 * 로컬 저장소 IO 가 실패해도 로그아웃 자체를 실패로 만들지 않는다(취소는 재던진다).
 * [ParfaitGroupRepository.clearGroups] 는 인메모리라 IO 실패 경로가 없어
 * [runSuspendCatching] 이 필요 없다.
 *
 * 그래서 [ParfaitGroupRepository.clearGroups] 를 먼저 부른다 — 던지지 않는 정리를 앞세워,
 * 뒤이은 [MemberRepository.clearMyAccount] 의 DataStore IO 가 취소를 재던지더라도(
 * [runSuspendCatching] 은 취소는 그대로 던진다) 그룹 캐시 정리까지 막지 않게 한다. 순서가
 * 반대면, 계정 정리 중 취소됐을 때 프로세스는 살아 있는 채 계정만 바뀌어 이전 계정의 그룹이
 * 캐시에 남는다. `TokenAuthenticator` 도 같은 근거로 같은 순서를 쓴다.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val result = authRepository.logout()
        parfaitGroupRepository.clearGroups()
        runSuspendCatching { memberRepository.clearMyAccount() }
        return result
    }
}
