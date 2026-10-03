package com.teamyg.parfait.feature.groups.list.impl.route.component

import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListEmptyIntroPhase.Dismissed
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListEmptyIntroPhase.Dismissing
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListEmptyIntroPhase.Entering
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListEmptyIntroPhase.Shown
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

    @Test
    fun timeline_matchesThePolicy() {
        assertEquals(listOf(500, 1000, 1500), (0..2).map(GroupListEmptyIntroTimeline::dummyDelayMillis))
        assertEquals(1000, GroupListEmptyIntroTimeline.DUMMY_DURATION_MILLIS)
        assertEquals(2500, GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS)
        assertEquals(500, GroupListEmptyIntroTimeline.TOOLTIP_DURATION_MILLIS)
        assertEquals(3000, GroupListEmptyIntroTimeline.ENTER_TOTAL_MILLIS)
        assertEquals(300, GroupListEmptyIntroTimeline.EXIT_DURATION_MILLIS)
    }

    @Test
    fun timeline_tooltipStartsWhenTheLastDummyLands_andEndsTheEntrance() {
        // Given 마지막 더미의 착지 시각
        val lastDummyEnd = GroupListEmptyIntroTimeline.dummyDelayMillis(2) +
            GroupListEmptyIntroTimeline.DUMMY_DURATION_MILLIS

        // When/Then 툴팁이 그때 시작하고, 툴팁이 끝나는 때가 등장의 끝이다
        assertEquals(GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS, lastDummyEnd)
        assertEquals(
            GroupListEmptyIntroTimeline.ENTER_TOTAL_MILLIS,
            GroupListEmptyIntroTimeline.TOOLTIP_DELAY_MILLIS + GroupListEmptyIntroTimeline.TOOLTIP_DURATION_MILLIS,
        )
    }

    @Test
    fun tooltipEnterEasing_followsTheFigmaSpringSamples() {
        val easing = GroupListEmptyIntroTimeline.TooltipEnterEasing

        assertEquals(0f, easing.transform(0f))
        assertEquals(1f, easing.transform(1f))
        assertEquals(0.3076f, easing.transform(0.1f), 0.0005f)
        assertEquals(0.9754f, easing.transform(0.5f), 0.0005f)
        // 샘플 사이는 선형으로 잇는다
        assertEquals((0.0216f + 0.0747f) / 2, easing.transform(0.03f), 0.0005f)
    }
}
