package com.teamyg.parfait.core.ui.reveal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TOTAL = 4

class StaggeredRevealTest {
    @Test
    fun isStaggerRevealed_nothingRevealedYet_hidesEveryItem() {
        // Given 아직 하나도 안 나왔다
        repeat(TOTAL) { index ->
            assertFalse(isStaggerRevealed(index = index, total = TOTAL, revealedCount = 0))
        }
    }

    @Test
    fun isStaggerRevealed_firstStep_showsBottomItemOnly() {
        // Given 한 개만 나왔다
        // Then 아래에서 위로 쌓이므로 마지막 자리가 먼저 나온다
        assertTrue(isStaggerRevealed(index = TOTAL - 1, total = TOTAL, revealedCount = 1))
        assertFalse(isStaggerRevealed(index = TOTAL - 2, total = TOTAL, revealedCount = 1))
        assertFalse(isStaggerRevealed(index = 0, total = TOTAL, revealedCount = 1))
    }

    @Test
    fun isStaggerRevealed_midway_showsOnlyItemsBelow() {
        // Given 절반이 나왔다
        assertTrue(isStaggerRevealed(index = TOTAL - 1, total = TOTAL, revealedCount = 2))
        assertTrue(isStaggerRevealed(index = TOTAL - 2, total = TOTAL, revealedCount = 2))
        assertFalse(isStaggerRevealed(index = TOTAL - 3, total = TOTAL, revealedCount = 2))
    }

    @Test
    fun isStaggerRevealed_allSteps_showsEveryItem() {
        // Given 다 나왔다
        repeat(TOTAL) { index ->
            assertTrue(isStaggerRevealed(index = index, total = TOTAL, revealedCount = TOTAL))
        }
    }
}
