package com.teamyg.parfait.data.session

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.session.SessionEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionEventBusTest {
    @Test
    fun postForcedLogout_beforeSubscribe_stillDelivers() = runTest {
        // Given 아직 아무도 구독하지 않은 버스
        val bus = SessionEventBus()

        // When 이벤트를 발행한 뒤에 구독한다
        bus.postForcedLogout()

        // Then 버퍼에 남아 있다가 전달된다 — 앱 루트가 붙기 전에 401이 나도 잃지 않는다
        bus.events.test {
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
        }
    }

    /**
     * 이 테스트가 보장하는 건 "발행이 두 번이어도 수신은 한 번" 뿐이다 — 이동이 중복
     * 실행되면 안 된다는 요구사항이 그것이다. `ForcedLogout` 이 페이로드 없는 싱글턴이라,
     * 이 결과만으로 `CONFLATED`(drop-oldest)와 `Channel(1)` + 실패한 `trySend` 무시
     * (drop-newest)를 구분할 수는 없다 — 둘 다 이 테스트를 통과한다.
     */
    @Test
    fun postForcedLogout_calledTwice_deliversOnce() = runTest {
        // Given 401 이 연달아 터져 이벤트가 두 번 발행된 버스
        val bus = SessionEventBus()
        bus.postForcedLogout()
        bus.postForcedLogout()

        // When 구독한다
        bus.events.test {
            // Then 한 번만 온다 — 이동이 두 번 일어나면 안 된다
            assertEquals(SessionEvent.ForcedLogout, awaitItem())
            expectNoEvents()
        }
    }
}
