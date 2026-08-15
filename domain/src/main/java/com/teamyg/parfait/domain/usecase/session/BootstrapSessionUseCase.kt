package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.session.SessionBootstrap
import com.teamyg.parfait.domain.repository.auth.AuthRepository
import com.teamyg.parfait.domain.repository.member.MemberRepository
import javax.inject.Inject

/**
 * 앱 진입(스플래시)에서 저장된 세션으로 자동로그인을 시도할지 판단한다.
 *
 * 토큰이 없으면 서버를 부르지 않고 바로 로그인으로 보낸다. 토큰이 있으면
 * [MemberRepository.refreshMyAccount] 로 세션을 검증하면서 동시에 SSoT 를 채운다 —
 * 성공하면 그룹 목록에 SSoT 가 채워진 채로 도착한다.
 *
 * 실패했을 때 정리 범위는 실패 원인에 따라 갈린다:
 * - [AppError.Network] — 연결이 없을 뿐 **세션이 죽었다는 증거가 아니다.** 아무것도
 *   지우지 않는다 — 연결이 돌아온 뒤 다시 켜면 자동로그인이 성립해야 한다.
 * - 그 외(서버 거절·인증 실패·예상 밖 오류) — 세션이 확실히 죽었다고 보고
 *   [AuthRepository.logout] 과 [MemberRepository.clearMyAccount] 를 모두 호출한다.
 *
 * 만료된 access token 은 `TokenAuthenticator` 가 재발급하므로 여기서는 401 을 직접
 * 다루지 않는다 — 재발급까지 실패하면 그쪽이 이미 토큰을 지우고 `ForcedLogout` 을 쏜다.
 */
class BootstrapSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
) {
    suspend operator fun invoke(): SessionBootstrap {
        if (!authRepository.hasSession()) return SessionBootstrap.ToLogin

        return memberRepository.refreshMyAccount().fold(
            onSuccess = { SessionBootstrap.ToGroupList },
            onFailure = { error -> handleRefreshFailure(error) },
        )
    }

    private suspend fun handleRefreshFailure(error: Throwable): SessionBootstrap {
        if (error !is AppError.Network) {
            authRepository.logout()
            // 로컬 저장소 IO 가 실패해도 라우팅은 이미 ToLogin 으로 정해져 있다(취소는 재던진다).
            runSuspendCatching { memberRepository.clearMyAccount() }
        }
        return SessionBootstrap.ToLogin
    }
}
