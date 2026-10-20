package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToppingVideoTimelineTest {
    @Test
    fun canvasVideoFrameCount_always_isTwoSeconds() {
        // Then 30fps 로 2초
        assertEquals(60, VideoTimelineOptions.FRAME_COUNT)
    }

    @Test
    fun toppingPopProgresses_noToppings_isEmpty() {
        assertTrue(toppingPopProgresses(toppingCount = 0, frameIndex = 0).isEmpty())
    }

    @Test
    fun toppingPopProgresses_firstFrame_startsFirstToppingWithoutIntro() {
        // When 토핑 3개의 첫 프레임
        val progresses = toppingPopProgresses(toppingCount = 3, frameIndex = 0)

        // Then 도입 구간 없이 첫 토핑이 0초에 시작하고 나머지는 아직이다
        assertEquals(listOf(0f, 0f, 0f), progresses)
        assertTrue(toppingPopProgresses(toppingCount = 3, frameIndex = 1)[0] > 0f)
    }

    @Test
    fun toppingPopProgresses_threeToppings_spreadsStartsOverAppearWindow() {
        // When 토핑 3개 — 시작 시각은 0초, 0.5초, 1초
        val atSecondStart = toppingPopProgresses(toppingCount = 3, frameIndex = 15)
        val atThirdStart = toppingPopProgresses(toppingCount = 3, frameIndex = 30)

        // Then 앞 토핑은 팝인(0.25초)을 끝냈고 다음 토핑이 막 시작한다
        assertEquals(listOf(1f, 0f, 0f), atSecondStart)
        assertEquals(listOf(1f, 1f, 0f), atThirdStart)
    }

    @Test
    fun toppingPopProgresses_manyToppings_overlapsPops() {
        // When 토핑 12개 — 시작 간격 0.125초가 팝인 0.25초보다 짧다
        val progresses = toppingPopProgresses(toppingCount = 12, frameIndex = 6)

        // Then 두 토핑이 동시에 팝인 중이다
        assertEquals(2, progresses.count { it > 0f && it < 1f })
    }

    @Test
    fun toppingPopProgresses_lastFrame_showsEveryToppingSettled() {
        // When 토핑 60개의 마지막 프레임
        val progresses = toppingPopProgresses(toppingCount = 60, frameIndex = VideoTimelineOptions.FRAME_COUNT - 1)

        // Then 전부 팝인이 끝나 있다 — 이미지 저장물과 같은 장면이어야 한다
        assertTrue(progresses.all { it == 1f })
    }

    @Test
    fun toppingPopScale_atStart_isStartScale() {
        assertEquals(0.6f, toppingPopScale(0f), 0.0001f)
    }

    @Test
    fun toppingPopScale_atEnd_isOne() {
        assertEquals(1f, toppingPopScale(1f), 0.0001f)
    }

    @Test
    fun toppingPopScale_beforeEnd_overshootsAboveOne() {
        // Then 중간에 1을 넘겨야 튀어 오르는 인상이 난다
        assertTrue(toppingPopScale(0.6f) > 1f)
    }

    @Test
    fun toppingPopAlpha_always_isLinear() {
        assertEquals(0f, toppingPopAlpha(0f), 0.0001f)
        assertEquals(0.5f, toppingPopAlpha(0.5f), 0.0001f)
        assertEquals(1f, toppingPopAlpha(1f), 0.0001f)
    }
}
