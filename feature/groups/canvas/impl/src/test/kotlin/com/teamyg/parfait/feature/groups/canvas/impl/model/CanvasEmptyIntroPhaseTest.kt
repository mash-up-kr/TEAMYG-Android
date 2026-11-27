package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase.Dismissed
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase.Dismissing
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase.Entering
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase.Idle
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroPhase.Shown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CanvasEmptyIntroPhaseTest {
    @Test
    fun animationFinished_entering_becomesShown() {
        assertEquals(Shown, Entering.onAnimationFinished())
    }

    @Test
    fun tap_whileEntering_dismissesImmediately() {
        assertEquals(Dismissing, Entering.onTap())
    }

    @Test
    fun tap_whenShown_dismisses() {
        assertEquals(Dismissing, Shown.onTap())
    }

    @Test
    fun interaction_whileEntering_skipsToShown() {
        assertEquals(Shown, Entering.onInteraction())
    }

    @Test
    fun interaction_whenShown_dismisses() {
        assertEquals(Dismissing, Shown.onInteraction())
    }

    @Test
    fun stop_whileEntering_skipsToShown() {
        assertEquals(Shown, Entering.onStop())
    }

    @Test
    fun stop_whenShown_staysShown() {
        assertEquals(Shown, Shown.onStop())
    }

    @Test
    fun stop_whileDismissing_skipsToDismissed() {
        assertEquals(Dismissed, Dismissing.onStop())
    }

    @Test
    fun dismissFinished_dismissing_becomesDismissed() {
        assertEquals(Dismissed, Dismissing.onDismissFinished())
    }

    @Test
    fun idleAndDismissed_ignoreEveryEvent() {
        listOf(Idle, Dismissed).forEach { phase ->
            assertEquals(phase, phase.onAnimationFinished())
            assertEquals(phase, phase.onTap())
            assertEquals(phase, phase.onInteraction())
            assertEquals(phase, phase.onStop())
            assertEquals(phase, phase.onDismissFinished())
        }
    }

    @Test
    fun enterValue_followsPhase() {
        assertEquals(0f, Idle.enterValue(0.4f))
        assertEquals(0.4f, Entering.enterValue(0.4f))
        assertEquals(1f, Shown.enterValue(0.4f))
        assertEquals(0.4f, Dismissing.enterValue(0.4f))
        assertEquals(0f, Dismissed.enterValue(0.4f))
    }

    @Test
    fun exitValue_followsPhase() {
        assertEquals(1f, Idle.exitValue(0.4f))
        assertEquals(1f, Entering.exitValue(0.4f))
        assertEquals(1f, Shown.exitValue(0.4f))
        assertEquals(0.4f, Dismissing.exitValue(0.4f))
        assertEquals(0f, Dismissed.exitValue(0.4f))
    }

    @Test
    fun isPlaying_onlyWhileIntroIsOnScreen() {
        assertFalse(Idle.isPlaying)
        assertTrue(Entering.isPlaying)
        assertTrue(Shown.isPlaying)
        assertTrue(Dismissing.isPlaying)
        assertFalse(Dismissed.isPlaying)
    }
}
