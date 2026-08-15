package com.teamyg.parfait.domain.usecase.session

import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
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
 * 실패했을 때 정리 범위는 **실패가 실제로 "세션이 죽었다"고 말하는지**로 갈린다
 * ([isAuthRejection]):
 * - 인증 거절(401, 또는 회원 조회 자체가 회원을 못 찾는다는 서버 코드) — 세션이
 *   확실히 죽었다고 보고 [AuthRepository.logout] 과 [MemberRepository.clearMyAccount]
 *   를 모두 호출한다.
 * - 그 외 전부([AppError.Network], 5xx 등 [AppError.Unexpected], 로컬 저장 실패 포함) —
 *   **아무것도 지우지 않는다.** 서버 장애·배포 중 순단으로 매번 로그아웃되면 안 되고,
 *   `saveLocally` 실패처럼 서버는 200 을 줬는데 로컬 쓰기만 실패한 경우 건강한 세션을
 *   지워서도 안 된다. 라우팅은 두 경우 모두 [SessionBootstrap.ToLogin] 으로 사용자
 *   입장에서는 동일하다 — 달라지는 건 세션을 파괴하느냐 뿐이다.
 *
 * 만료된 access token 은 `TokenAuthenticator` 가 재발급하므로 여기서는 401 을 직접
 * 다루지 않는다 — 재발급까지 실패하면 그쪽이 이미 토큰을 지우고 `ForcedLogout` 을 쏜다.
 * 그럼에도 여기서 401 을 또 판정하는 이유는, 그 재발급 경로를 타지 않는 순수 조회 실패
 * (예: 액세스 토큰은 살아있는데 회원이 서버에서 사라진 경우)까지 놓치지 않기 위해서다.
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
        if (error.isAuthRejection()) {
            authRepository.logout()
            // 로컬 저장소 IO 가 실패해도 라우팅은 이미 ToLogin 으로 정해져 있다(취소는 재던진다).
            runSuspendCatching { memberRepository.clearMyAccount() }
        }
        return SessionBootstrap.ToLogin
    }

    /**
     * 이 실패가 "세션이 죽었다"고 말하는 실패인가.
     *
     * [AppError.Server] 이면서 HTTP 401 이거나, 회원 조회(`/api/v1/users/me`) 도메인이
     * "그 회원이 없다"고 답한 경우([ServerErrorCode.Member.MEMBER_NOT_FOUND])만 그렇다.
     * 이 둘은 서버가 **자격증명 자체를 거절**했다는 뜻이라 세션을 계속 들고 있는 게
     * 오히려 틀렸다 — 나머지(연결 실패·5xx·로컬 IO 등)는 인증과 무관한 사고라
     * 세션의 생사를 말해주지 않는다.
     */
    private fun Throwable.isAuthRejection(): Boolean = this is AppError.Server &&
        (statusCode == HTTP_UNAUTHORIZED || code == ServerErrorCode.Member.MEMBER_NOT_FOUND)

    private companion object {
        const val HTTP_UNAUTHORIZED = 401
    }
}
