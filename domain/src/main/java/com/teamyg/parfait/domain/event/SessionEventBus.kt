package com.teamyg.parfait.domain.event

import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.flow.Flow

/**
 * **이 Bus 는 구독만 내놓는다** — 발행(`postForcedLogout`)은 `:data` 구현에만 있다.
 * [PushDeepLinkEventBus] 가 `post` 를 계약에 함께 두는 것과 다른데, 세션 종료를 발행하는
 * 자리가 `:data` 안(`TokenAuthenticator`)뿐이라 밖으로 낼 이유가 없기 때문이다.
 *
 * **앱 루트 한 곳에서만 수집한다** — 화면마다 구독하면 한 이벤트로 여러 번 이동한다.
 * 구현이 `Channel` 기반이라 실제로도 단일 소비자다.
 */
interface SessionEventBus {
    val events: Flow<SessionEvent>
}
