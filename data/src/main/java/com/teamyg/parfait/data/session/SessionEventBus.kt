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

    /**
     * 확인을 남겨두는 이유: `CONFLATED` 인 지금은 [kotlinx.coroutines.channels.Channel.trySend]
     * 가 버퍼 때문에 실패할 수 없다(가장 오래된 값을 밀어내고 항상 성공한다). 채널 종류가
     * 바뀌면 그때부터 조용한 유실이 생기므로, 그 회귀가 로그로 드러나게 확인만 남긴다.
     */
    fun postForcedLogout() {
        if (channel.trySend(SessionEvent.ForcedLogout).isFailure) {
            sourceLogger.e { "SessionEvent.ForcedLogout 을 전달하지 못했다 — CONFLATED 채널 가정이 깨졌다" }
        }
    }
}
