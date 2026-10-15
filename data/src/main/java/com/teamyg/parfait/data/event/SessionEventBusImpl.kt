package com.teamyg.parfait.data.event

import com.teamyg.parfait.data.utils.sourceLogger
import com.teamyg.parfait.domain.model.session.SessionEvent
import com.teamyg.parfait.domain.event.SessionEventBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `Channel(CONFLATED)` 라 앱 루트가 수집을 시작하기 전에 난 401 도 버퍼에 남았다가 전달되고,
 * 401 이 여러 건 터져도 이동은 한 번으로 접힌다. `SharedFlow` + `replay` 는 이미 소비한
 * 이벤트가 재구독 때 다시 와서 이동이 반복된다(ADR-0020).
 */
@Singleton
class SessionEventBusImpl @Inject constructor() : SessionEventBus {
    private val channel = Channel<SessionEvent>(Channel.CONFLATED)

    override val events: Flow<SessionEvent> = channel.receiveAsFlow()

    fun postForcedLogout() {
        if (channel.trySend(SessionEvent.ForcedLogout).isFailure) {
            sourceLogger.e { "SessionEvent.ForcedLogout 을 전달하지 못했다 — CONFLATED 채널 가정이 깨졌다" }
        }
    }
}
