package com.teamyg.parfait.feature.groups.list.impl.model

import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroPhase.Dismissed
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroPhase.Dismissing
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroPhase.Entering
import com.teamyg.parfait.feature.groups.list.impl.model.GroupListEmptyIntroPhase.Shown
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupListEmptyIntroPhaseTest {
    @Test
    fun onTouchDown_whileEntering_staysEntering() {
        assertEquals(Entering, Entering.onTouchDown())
    }

    @Test
    fun onTouchDown_whileShown_startsDismissing() {
        assertEquals(Dismissing, Shown.onTouchDown())
    }

    @Test
    fun onTouchDown_whileDismissingOrDismissed_keepsPhase() {
        assertEquals(Dismissing, Dismissing.onTouchDown())
        assertEquals(Dismissed, Dismissed.onTouchDown())
    }

    @Test
    fun onStop_skipsTheRunningAnimationToItsEnd() {
        assertEquals(Shown, Entering.onStop())
        assertEquals(Dismissed, Dismissing.onStop())
    }

    @Test
    fun onStop_whileSettled_keepsPhase() {
        assertEquals(Shown, Shown.onStop())
        assertEquals(Dismissed, Dismissed.onStop())
    }

    @Test
    fun enterValue_followsTheAnimationOnlyWhileEntering() {
        assertEquals(0.3f, Entering.enterValue(0.3f))
        assertEquals(1f, Shown.enterValue(0.3f))
        assertEquals(1f, Dismissing.enterValue(0.3f))
        assertEquals(1f, Dismissed.enterValue(0.3f))
    }

    @Test
    fun exitValue_followsTheAnimationOnlyWhileDismissing() {
        assertEquals(1f, Entering.exitValue(0.4f))
        assertEquals(1f, Shown.exitValue(0.4f))
        assertEquals(0.4f, Dismissing.exitValue(0.4f))
        assertEquals(0f, Dismissed.exitValue(0.4f))
    }
}
