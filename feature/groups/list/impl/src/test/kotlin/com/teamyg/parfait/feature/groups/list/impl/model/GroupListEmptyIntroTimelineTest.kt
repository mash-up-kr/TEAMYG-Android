package com.teamyg.parfait.feature.groups.list.impl.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GroupListEmptyIntroTimelineTest {
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
