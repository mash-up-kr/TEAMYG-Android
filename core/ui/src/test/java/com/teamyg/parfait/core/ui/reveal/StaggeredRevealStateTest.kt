package com.teamyg.parfait.core.ui.reveal

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val STEP_MILLIS = 400L
private const val TOTAL = 3

@OptIn(ExperimentalCoroutinesApi::class)
class StaggeredRevealStateTest {
    @Test
    fun reveal_justStarted_showsTopItemWithoutWaiting() = runTest {
        val state = StaggeredRevealState(STEP_MILLIS)

        backgroundScope.launch { state.reveal(TOTAL) }
        runCurrent()

        assertTrue(state.isRevealed(0))
        assertFalse(state.isRevealed(1))
    }

    @Test
    fun reveal_oneStepPassed_showsNextItem() = runTest {
        val state = StaggeredRevealState(STEP_MILLIS)

        backgroundScope.launch { state.reveal(TOTAL) }
        advanceTimeBy(STEP_MILLIS)
        runCurrent()

        assertTrue(state.isRevealed(1))
        assertFalse(state.isRevealed(2))
    }

    @Test
    fun reveal_everyStepPassed_showsEveryItem() = runTest {
        val state = StaggeredRevealState(STEP_MILLIS)

        backgroundScope.launch { state.reveal(TOTAL) }
        advanceTimeBy(STEP_MILLIS * TOTAL)
        runCurrent()

        repeat(TOTAL) { index ->
            assertTrue(state.isRevealed(index))
        }
    }

    @Test
    fun reveal_itemsAdded_keepsAlreadyShownItems() = runTest {
        val state = StaggeredRevealState(STEP_MILLIS)

        backgroundScope.launch { state.reveal(TOTAL) }
        advanceTimeBy(STEP_MILLIS * TOTAL)
        runCurrent()

        backgroundScope.launch { state.reveal(TOTAL + 2) }
        runCurrent()

        repeat(TOTAL) { index ->
            assertTrue(state.isRevealed(index))
        }
        assertFalse(state.isRevealed(TOTAL + 1))
    }

    @Test
    fun revealAll_neverRun_showsEveryItemAtOnce() = runTest {
        val state = StaggeredRevealState(STEP_MILLIS)

        state.revealAll()

        assertTrue(state.isRevealed(0))
        assertTrue(state.isRevealed(TOTAL * 10))
    }
}
