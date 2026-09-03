package com.teamyg.parfait.push

import android.content.Intent
import com.teamyg.parfait.domain.model.push.PushDeepLink
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushDeepLinkIntentTest {
    private fun intent(route: String?, groupId: String?): Intent = mockk {
        every { getStringExtra("route") } returns route
        every { getStringExtra("groupId") } returns groupId
    }

    @Test
    fun toPushDeepLinkOrNull_routeCanvasWithValidGroupId_returnsAddTopping() {
        // Given, When route=canvas, groupId 가 양수인 알림
        val result = intent(route = "canvas", groupId = "50").toPushDeepLinkOrNull()

        // Then 그 그룹으로 향하는 AddTopping
        assertEquals(PushDeepLink.AddTopping(groupId = 50L), result)
    }

    @Test
    fun toPushDeepLinkOrNull_routeCanvasWithZeroGroupId_returnsNull() {
        // Given, When groupId 가 0(경계값)
        val result = intent(route = "canvas", groupId = "0").toPushDeepLinkOrNull()

        // Then 유효하지 않은 그룹이라 딥링크로 보지 않는다
        assertNull(result)
    }

    @Test
    fun toPushDeepLinkOrNull_routeCanvasWithNegativeGroupId_returnsNull() {
        // Given, When groupId 가 음수
        val result = intent(route = "canvas", groupId = "-1").toPushDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toPushDeepLinkOrNull_routeCanvasWithNonNumericGroupId_returnsNull() {
        // Given, When groupId 가 숫자가 아님(FCM data 는 전부 String 이라 실제로 벌어질 수 있다)
        val result = intent(route = "canvas", groupId = "abc").toPushDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toPushDeepLinkOrNull_routeCanvasWithMissingGroupId_returnsNull() {
        // Given, When groupId 자체가 없음
        val result = intent(route = "canvas", groupId = null).toPushDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toPushDeepLinkOrNull_routeGroup_returnsReminder() {
        // Given, When route=group
        val result = intent(route = "group", groupId = null).toPushDeepLinkOrNull()

        // Then
        assertEquals(PushDeepLink.Reminder, result)
    }

    @Test
    fun toPushDeepLinkOrNull_unknownRoute_returnsNull() {
        // Given, When 모르는 route 값(향후 필드 추가 시 구버전 앱 호환)
        val result = intent(route = "unknown", groupId = null).toPushDeepLinkOrNull()

        // Then
        assertNull(result)
    }

    @Test
    fun toPushDeepLinkOrNull_noRoute_returnsNull() {
        // Given, When 딥링크 extras 자체가 없는 평범한 실행
        val result = intent(route = null, groupId = null).toPushDeepLinkOrNull()

        // Then
        assertNull(result)
    }
}
