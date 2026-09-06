package com.teamyg.parfait.push

import android.content.Intent
import com.teamyg.parfait.domain.model.push.PushDeepLink
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushDeepLinkIntentTest {
    @Test
    fun toPushDeepLinkOrNull_freshNotificationTap_returnsDeepLink() {
        // Given, When 알림을 막 탭해 들어온 인텐트
        val result = pushIntent(flags = FLAGS_FRESH_TAP).toPushDeepLinkOrNull()

        // Then
        assertEquals(PushDeepLink.AddTopping(groupId = 86L), result)
    }

    @Test
    fun toPushDeepLinkOrNull_relaunchedFromHistory_returnsNull() {
        // Given, When 같은 인텐트가 태스크의 base intent 로 다시 전달됐다(최근 앱·런처로 되살림)
        val result = pushIntent(
            flags = FLAGS_FRESH_TAP or Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY,
        ).toPushDeepLinkOrNull()

        // Then 지금 알림을 탭한 것이 아니므로 딥링크로 보지 않는다
        assertNull(result)
    }

    private fun pushIntent(flags: Int): Intent = mockk {
        every { this@mockk.flags } returns flags
        every { getStringExtra("route") } returns "canvas"
        every { getStringExtra("groupId") } returns "86"
        every { getStringExtra("type") } returns "TOPPING"
    }

    private companion object {
        /** 실기기에서 알림을 탭했을 때 실제로 찍힌 값. */
        const val FLAGS_FRESH_TAP = 0x14000000
    }
}
