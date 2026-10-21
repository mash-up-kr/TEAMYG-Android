package com.teamyg.parfait.data.event

import app.cash.turbine.test
import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLinkDeepLinkEventBusImplTest {
    @Test
    fun post_beforeSubscribe_stillDelivers() = runTest {
        // Given 아직 아무도 구독하지 않은 버스
        val bus = AppLinkDeepLinkEventBusImpl()

        // When 딥링크를 발행한 뒤에 구독한다
        bus.post(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"))

        // Then 버퍼에 남아 있다가 전달된다
        bus.deepLinks.test {
            assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"), awaitItem())
        }
    }

    @Test
    fun post_calledTwice_deliversOnlyTheLatest() = runTest {
        // Given 두 번 연달아 발행된 상황
        val bus = AppLinkDeepLinkEventBusImpl()
        bus.post(AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123"))
        bus.post(AppLinkDeepLink.OpenGroupInvite(inviteCode = "XYZ789"))

        // When 구독한다
        bus.deepLinks.test {
            // Then 마지막 것 하나로 접힌다
            assertEquals(AppLinkDeepLink.OpenGroupInvite(inviteCode = "XYZ789"), awaitItem())
            expectNoEvents()
        }
    }
}
