package com.teamyg.parfait.core.ui.gesture

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HiddenTapCounterTest {
    @Test
    fun tap_beforeRequiredCount_returnsFalse() {
        val counter = HiddenTapCounter()

        repeat(4) { assertFalse(counter.tap()) }
    }

    @Test
    fun tap_atRequiredCount_returnsTrue() {
        val counter = HiddenTapCounter()
        repeat(4) { counter.tap() }

        assertTrue(counter.tap())
    }

    @Test
    fun tap_afterTrigger_countsFromZeroAgain() {
        // Given 5번째에서 한 번 열렸다
        val counter = HiddenTapCounter()
        repeat(5) { counter.tap() }

        // Then 다시 4번은 false, 그 뒤 5번째(누적 10번째)에서 true
        repeat(4) { assertFalse(counter.tap()) }
        assertTrue(counter.tap())
    }

    @Test
    fun tap_customRequired_triggersAtThatCount() {
        val counter = HiddenTapCounter(required = 2)

        assertFalse(counter.tap())
        assertTrue(counter.tap())
    }
}
