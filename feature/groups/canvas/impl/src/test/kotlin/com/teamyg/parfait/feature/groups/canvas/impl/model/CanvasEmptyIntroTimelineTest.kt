package com.teamyg.parfait.feature.groups.canvas.impl.model

import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_DOWN_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_TEXT
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.HIGHLIGHT_UP_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.IMAGE_COUNT
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.IMAGE_DURATION_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.TOTAL_MILLIS
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.highlightDelayMillis
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasEmptyIntroTimeline.imageDelayMillis
import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasEmptyIntroTimelineTest {
    @Test
    fun imageDelays_are500msApart_startingAt500() {
        assertEquals(listOf(500, 1000, 1500), (0 until IMAGE_COUNT).map(::imageDelayMillis))
    }

    @Test
    fun lastImage_finishesAtTotal() {
        assertEquals(2500, imageDelayMillis(IMAGE_COUNT - 1) + IMAGE_DURATION_MILLIS)
        assertEquals(2500, TOTAL_MILLIS)
    }

    @Test
    fun highlightText_hasNineNonSpaceCharacters() {
        assertEquals(9, HIGHLIGHT_TEXT.count { it != ' ' })
    }

    @Test
    fun highlightDelays_startAt500_andStep150() {
        assertEquals(500, highlightDelayMillis(0))
        assertEquals(650, highlightDelayMillis(1))
    }

    @Test
    fun lastHighlightedCharacter_returnsToGrayAt2150() {
        val lastIndex = HIGHLIGHT_TEXT.count { it != ' ' } - 1

        assertEquals(2150, highlightDelayMillis(lastIndex) + HIGHLIGHT_UP_MILLIS + HIGHLIGHT_DOWN_MILLIS)
    }
}
