package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToppingRevealTest {
    @Test
    fun allToppingsSettled_noToppingsYet_isNotSettled() {
        // Given 캔버스 조회가 오기 전이라 그릴 토핑이 아직 없다
        val settled = emptyList<Boolean>()

        // Then 이 순간을 완료로 세면 빗장이 먼저 풀려, 뒤늦게 온 토핑이 하나씩 뜬다
        assertFalse(allToppingsSettled(settled))
    }

    @Test
    fun allToppingsSettled_oneStillLoading_isNotSettled() {
        // Given 한 장이 아직 오는 중이다
        val settled = listOf(true, false, true)

        // Then
        assertFalse(allToppingsSettled(settled))
    }

    @Test
    fun allToppingsSettled_everyToppingDone_isSettled() {
        // Given 전부 결말났다
        val settled = listOf(true, true, true)

        // Then
        assertTrue(allToppingsSettled(settled))
    }
}
