package com.teamyg.parfait.data.push

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.push.PushDeepLink
import com.teamyg.parfait.domain.model.push.PushNotificationType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PushDeepLinkEventBusImplTest {
    @Test
    fun post_beforeSubscribe_stillDelivers() = runTest {
        // Given 아직 아무도 구독하지 않은 버스
        val bus = PushDeepLinkEventBusImpl()

        // When 딥링크를 발행한 뒤에 구독한다
        bus.post(PushDeepLink.AddTopping(groupId = 1L))

        // Then 버퍼에 남아 있다가 전달된다 — 로그인 전에 탭해도 잃지 않는다
        bus.deepLinks.test {
            assertEquals(PushDeepLink.AddTopping(groupId = 1L), awaitItem())
        }
    }

    @Test
    fun post_calledTwice_deliversOnlyTheLatest() = runTest {
        // Given 알림을 연달아 두 번 탭한 상황
        val bus = PushDeepLinkEventBusImpl()
        bus.post(PushDeepLink.AddTopping(groupId = 1L))
        bus.post(PushDeepLink.GroupList(type = PushNotificationType.REMIND_AM))

        // When 구독한다
        bus.deepLinks.test {
            // Then 마지막 것 하나로 접힌다 — 화면은 결국 한 곳에만 도착해야 한다
            assertEquals(PushDeepLink.GroupList(type = PushNotificationType.REMIND_AM), awaitItem())
            expectNoEvents()
        }
    }
}
