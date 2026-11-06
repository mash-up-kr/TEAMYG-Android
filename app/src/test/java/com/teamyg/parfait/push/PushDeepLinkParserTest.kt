package com.teamyg.parfait.push

import com.teamyg.parfait.domain.model.push.PushDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushDeepLinkParserTest {
    @Test
    fun parse_routeCanvasWithValidGroupId_returnsAddTopping() {
        // Given, When route=canvas, groupId 가 양수인 알림
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = "50", type = "TOPPING")

        // Then 그 그룹으로 향하는 AddTopping
        assertEquals(PushDeepLink.AddTopping(groupId = 50L, type = "TOPPING"), result)
    }

    @Test
    fun parse_routeCanvasWithServerType_keepsServerTypeAsIs() {
        // Given, When route=canvas 인데 type 이 TOPPING 이 아닌 서버 값
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = "50", type = "BACKGROUND")

        // Then 앱이 TOPPING 으로 덮어쓰지 않고 서버 값을 그대로 둔다
        assertEquals(PushDeepLink.AddTopping(groupId = 50L, type = "BACKGROUND"), result)
    }

    @Test
    fun parse_routeCanvasWithZeroGroupId_returnsNull() {
        // Given, When groupId 가 0(경계값)
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = "0", type = "TOPPING")

        // Then 유효하지 않은 그룹이라 딥링크로 보지 않는다
        assertNull(result)
    }

    @Test
    fun parse_routeCanvasWithNegativeGroupId_returnsNull() {
        // Given, When groupId 가 음수
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = "-1", type = "TOPPING")

        // Then
        assertNull(result)
    }

    @Test
    fun parse_routeCanvasWithNonNumericGroupId_returnsNull() {
        // Given, When groupId 가 숫자가 아님(FCM data 는 전부 String 이라 실제로 벌어질 수 있다)
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = "abc", type = "TOPPING")

        // Then
        assertNull(result)
    }

    @Test
    fun parse_routeCanvasWithMissingGroupId_returnsNull() {
        // Given, When groupId 자체가 없음
        val result = PushDeepLinkParser.parse(route = "canvas", groupId = null, type = "TOPPING")

        // Then
        assertNull(result)
    }

    @Test
    fun parse_routeGroupWithTypeRemindAm_returnsGroupListWithThatType() {
        // Given, When route=group, type=REMIND_AM
        val result = PushDeepLinkParser.parse(route = "group", groupId = null, type = "REMIND_AM")

        // Then type 을 그대로 들고 있는 GroupList — 라우팅엔 안 쓰지만 탭 분석 등에 남겨 둔다
        assertEquals(PushDeepLink.GroupList(type = "REMIND_AM"), result)
    }

    @Test
    fun parse_routeGroupWithTypeRemindPm_returnsGroupListWithThatType() {
        // Given, When route=group, type=REMIND_PM
        val result = PushDeepLinkParser.parse(route = "group", groupId = null, type = "REMIND_PM")

        // Then
        assertEquals(PushDeepLink.GroupList(type = "REMIND_PM"), result)
    }

    @Test
    fun parse_routeGroupWithUnknownType_keepsServerTypeAsIs() {
        // Given, When type 이 앞으로 서버가 추가할 값이라 앱이 모르는 문자열
        val result = PushDeepLinkParser.parse(route = "group", groupId = null, type = "REMIND_NOON")

        // Then 서버 값을 그대로 들고 있고, 라우팅도 route 만으로 이미 결정돼 있어 실패하지 않는다
        assertEquals(PushDeepLink.GroupList(type = "REMIND_NOON"), result)
    }

    @Test
    fun parse_routeGroupWithMissingType_returnsGroupListWithNullType() {
        // Given, When type 자체가 없음
        val result = PushDeepLinkParser.parse(route = "group", groupId = null, type = null)

        // Then
        assertEquals(PushDeepLink.GroupList(type = null), result)
    }

    @Test
    fun parse_unknownRoute_returnsNull() {
        // Given, When 모르는 route 값(향후 필드 추가 시 구버전 앱 호환)
        val result = PushDeepLinkParser.parse(route = "unknown", groupId = null, type = null)

        // Then
        assertNull(result)
    }

    @Test
    fun parse_noRoute_returnsNull() {
        // Given, When 딥링크 extras 자체가 없는 평범한 실행
        val result = PushDeepLinkParser.parse(route = null, groupId = null, type = null)

        // Then
        assertNull(result)
    }
}
