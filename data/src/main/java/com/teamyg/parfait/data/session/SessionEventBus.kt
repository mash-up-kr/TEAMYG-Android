package com.teamyg.parfait.data.session

import com.teamyg.parfait.data.utils.sourceLogger
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
 * 다만 `ForcedLogout` 이 페이로드 없는 싱글턴이라, 이 드롭 정책(drop-oldest) 자체가
 * 테스트로 증명되는 건 아니다 — `Channel(1)` + 실패한 `trySend` 무시(drop-newest)도
 * 관측상 똑같이 "여러 번 발행해도 한 번만 수신"을 만족한다. 테스트가 실제로 보장하는
 * 건 그 성질뿐이고, `CONFLATED` 는 그걸 만족하는 가장 단순한 선택이라는 근거다.
 */
@Singleton
class SessionEventBus @Inject constructor() : SessionEventSource {
    private val channel = Channel<SessionEvent>(Channel.CONFLATED)

    override val events: Flow<SessionEvent> = channel.receiveAsFlow()

    fun postForcedLogout() {
        if (channel.trySend(SessionEvent.ForcedLogout).isFailure) {
            sourceLogger.e { "SessionEvent.ForcedLogout 발행 실패 — 버퍼가 가득 찼다" }
        }
    }
}
