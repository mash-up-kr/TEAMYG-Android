package com.teamyg.parfait.analytics

import com.teamyg.parfait.domain.model.deeplink.AppLinkDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLinkOpenedEventTest {
    @Test
    fun toOpenedEvent_groupInvite_matchesTheIosPayload() {
        // Given 초대 링크로 열린 경우
        val deepLink = AppLinkDeepLink.OpenGroupInvite(inviteCode = "ABC123")

        // When
        val event = deepLink.toOpenedEvent()

        // Then iOS 와 같은 이름과 목적지 값으로 나간다 — 대시보드에서 두 플랫폼을 합쳐 본다
        assertEquals("AppLink", event.type.eventName)
        assertEquals(mapOf("destination" to "group_invite"), event.params)
    }
}
