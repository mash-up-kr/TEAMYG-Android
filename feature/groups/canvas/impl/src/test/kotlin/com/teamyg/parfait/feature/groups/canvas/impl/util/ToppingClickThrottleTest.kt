package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingClickThrottleTest {
    @Test
    fun tryPass_sameKeyWithinWindow_isBlocked() {
        // Given 시각을 손으로 미는 게이트
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }

        // When 같은 대상을 창 안에서 두 번 누른다
        assertTrue(throttle.tryPass("a"))
        now = 100L

        // Then 두 번째는 막힌다
        assertFalse(throttle.tryPass("a"))
    }

    @Test
    fun tryPass_sameKeyAfterWindow_passes() {
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }

        assertTrue(throttle.tryPass("a"))
        now = 300L

        assertTrue(throttle.tryPass("a"))
    }

    @Test
    fun tryPass_differentKeyWithinWindow_passes() {
        // Given 토핑을 눌러 스포트라이트를 켠 직후
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }
        assertTrue(throttle.tryPass("topping"))
        now = 50L

        // When 곧바로 바깥을 누른다
        // Then 대상이 다르므로 즉시 통과한다 — 해제가 씹히면 안 된다
        assertTrue(throttle.tryPass("dim"))
    }

    @Test
    fun tryPass_threeCallSequence_correctlyUpdatesTimeOnly_onPass() {
        // Given 시각을 손으로 미는 게이트
        var now = 0L
        val throttle = ToppingClickThrottle(windowMillis = 300L) { now }

        // When 같은 대상을 세 번 누른다
        // 1번: 창이 열린 시점에 통과한다
        assertTrue(throttle.tryPass("a"))

        // 2번: 창 안에서 다시 눌러 막힌다
        now = 150L
        assertFalse(throttle.tryPass("a"))

        // 3번: 첫 통과로부터 창 길이가 지난 시점에 다시 눌러 통과해야 한다
        now = 300L

        // Then 막힌 호출이 마지막 시각을 갱신하지 않아야 통과한다
        assertTrue(throttle.tryPass("a"))
    }
}
