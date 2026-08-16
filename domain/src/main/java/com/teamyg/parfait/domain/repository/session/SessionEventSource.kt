package com.teamyg.parfait.domain.repository.session

import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.flow.Flow

/**
 * 세션 사건 구독구. **앱 루트 한 곳에서만 수집한다** — 화면마다 구독하면 한 이벤트로
 * 여러 번 이동한다. 구현이 `Channel` 기반이라 실제로도 단일 소비자다.
 */
interface SessionEventSource {
    val events: Flow<SessionEvent>
}
