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
            assertFalse(isStaggerRevealed(index = index, revealedCount = 0))
        }
    }

    @Test
    fun isStaggerRevealed_firstStep_showsTopItemOnly() {
        // Given 한 개만 나왔다
        // Then 파르페가 체리에서 컵 쪽으로 자라므로 맨 위 자리가 먼저 나온다
        assertTrue(isStaggerRevealed(index = 0, revealedCount = 1))
        assertFalse(isStaggerRevealed(index = 1, revealedCount = 1))
        assertFalse(isStaggerRevealed(index = TOTAL - 1, revealedCount = 1))
    }

    @Test
    fun isStaggerRevealed_midway_showsOnlyItemsAbove() {
        // Given 절반이 나왔다
        assertTrue(isStaggerRevealed(index = 1, revealedCount = 2))
        assertFalse(isStaggerRevealed(index = 2, revealedCount = 2))
    }

    @Test
    fun isStaggerRevealed_allSteps_showsEveryItem() {
        // Given 다 나왔다
        repeat(TOTAL) { index ->
            assertTrue(isStaggerRevealed(index = index, revealedCount = TOTAL))
        }
    }
}
