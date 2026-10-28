package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class ToppingVideoTimelineTest {
    @Test
    fun canvasVideoFrameCount_always_isTwoSeconds() {
        // Then 30fps 로 2초
        assertEquals(60, VideoTimelineOptions.FRAME_COUNT)
    }

    @Test
    fun visibleToppingCount_noToppings_isZero() {
        assertEquals(0, visibleToppingCount(toppingCount = 0, frameIndex = 0))
        assertEquals(0, visibleToppingCount(toppingCount = 0, frameIndex = VideoTimelineOptions.FRAME_COUNT - 1))
    }

    @Test
    fun visibleToppingCount_firstFrame_showsFirstToppingWithoutIntro() {
        assertEquals(1, visibleToppingCount(toppingCount = 3, frameIndex = 0))
    }

    @Test
    fun visibleToppingCount_threeToppings_appearsOnStartFrame() {
        // 시작 시각 0초·0.5초·1초 = 0·15·30 프레임. 시작 프레임에 바로 보인다
        assertEquals(1, visibleToppingCount(toppingCount = 3, frameIndex = 14))
        assertEquals(2, visibleToppingCount(toppingCount = 3, frameIndex = 15))
        assertEquals(2, visibleToppingCount(toppingCount = 3, frameIndex = 29))
        assertEquals(3, visibleToppingCount(toppingCount = 3, frameIndex = 30))
    }

    @Test
    fun visibleToppingCount_fromAppearWindowEnd_showsAll() {
        for (count in 1..200) {
            assertEquals(count, visibleToppingCount(count, VideoTimelineOptions.TOPPING_APPEAR_WINDOW_FRAMES))
            assertEquals(count, visibleToppingCount(count, VideoTimelineOptions.FRAME_COUNT - 1))
        }
    }

    @Test
    fun visibleToppingCount_acrossFrames_neverDecreases() {
        for (count in 1..200) {
            val counts = (0 until VideoTimelineOptions.FRAME_COUNT).map { visibleToppingCount(count, it) }
            assertEquals(counts.sorted(), counts)
        }
    }

    @Test
    fun visibleToppingCount_everyFrame_matchesIosBoundary() {
        // iOS CanvasImageExporter.toppingVisibilities 의 식을 그대로 옮긴 것
        for (count in 1..200) {
            for (frame in 0 until VideoTimelineOptions.FRAME_COUNT) {
                val time = frame.toDouble() / VideoTimelineOptions.FRAMES_PER_SECOND
                val ios = (0 until count).count { order -> time >= 1.5 * order / count }
                assertEquals("count=$count frame=$frame", ios, visibleToppingCount(count, frame))
            }
        }
    }
}
