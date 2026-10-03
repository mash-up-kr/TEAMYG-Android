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
        // Given 등장 중이다
        // When 터치하면
        // Then 단계가 그대로다
        assertEquals(Entering, Entering.onTouchDown())
    }

    @Test
    fun onTouchDown_whileShown_startsDismissing() {
        // Given 등장이 끝났다
        // When 터치하면
        // Then 종료가 시작된다
        assertEquals(Dismissing, Shown.onTouchDown())
    }

    @Test
    fun onTouchDown_whileDismissingOrDismissed_keepsPhase() {
        // Given 종료 중이거나 종료됐다
        // When 터치하면
        // Then 단계가 그대로다
        assertEquals(Dismissing, Dismissing.onTouchDown())
        assertEquals(Dismissed, Dismissed.onTouchDown())
    }

    @Test
    fun onStop_skipsTheRunningAnimationToItsEnd() {
        // Given 애니메이션이 진행 중이다
        // When 멈추면
        // Then 그 애니메이션의 끝 단계로 건너뛴다
        assertEquals(Shown, Entering.onStop())
        assertEquals(Dismissed, Dismissing.onStop())
    }

    @Test
    fun onStop_whileSettled_keepsPhase() {
        // Given 애니메이션이 이미 끝났다
        // When 멈추면
        // Then 단계가 그대로다
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
