package com.teamyg.parfait.data.session

import com.teamyg.parfait.domain.model.session.SessionEvent
import com.teamyg.parfait.domain.repository.session.SessionEventSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * `Channel(CONFLATED)` 인 이유는 두 가지다.
 *
 * 구독자가 없는 순간 발행해도 버퍼에 남았다가 전달돼야 한다 — 앱 루트가 수집을 시작하기
 * 전에 401 이 나도 잃으면 안 된다. `SharedFlow` + `replay` 는 이미 소비한 이벤트가
 * 재구독으로 다시 와서 이동이 저절로 반복된다(ADR-0020 이 이펙트에서 같은 이유로 기각).
 *
 * `CONFLATED` 는 401 이 여러 건 터져 이벤트가 연달아 발행돼도 이동을 한 번으로 접는다.
 */
@Singleton
class SessionEventBus @Inject constructor() : SessionEventSource {
    private val channel = Channel<SessionEvent>(Channel.CONFLATED)

    override val events: Flow<SessionEvent> = channel.receiveAsFlow()

    fun postForcedLogout() {
        channel.trySend(SessionEvent.ForcedLogout)
    }
}
