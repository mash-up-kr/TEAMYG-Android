package com.teamyg.parfait.feature.groups.canvas.impl.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToppingVideoTimelineTest {
    @Test
    fun toppingVideoFrames_noToppings_isIntroPlusOutroOnly() {
        // When 토핑이 없다
        val frames = toppingVideoFrames(toppingCount = 0)

        // Then 도입 15 + 마무리 30 프레임만 남는다
        assertEquals(45, frames.size)
        assertTrue(frames.all { it.visibleCount == 0 })
        assertTrue(frames.all { it.popProgress == 0f })
    }

    @Test
    fun toppingVideoFrames_threeToppings_countsIntroToppingsAndOutro() {
        // When 토핑 3개
        val frames = toppingVideoFrames(toppingCount = 3)

        // Then 15 + 12*3 + 30
        assertEquals(81, frames.size)
    }

    @Test
    fun toppingVideoFrames_introFrames_showNothing() {
        // When 토핑 1개
        val frames = toppingVideoFrames(toppingCount = 1)

        // Then 앞 15프레임은 배경만이다
        assertTrue(frames.take(15).all { it.visibleCount == 0 && it.popProgress == 0f })
    }

    @Test
    fun toppingVideoFrames_lastFrame_showsEveryToppingSettled() {
        // When 토핑 4개
        val frames = toppingVideoFrames(toppingCount = 4)

        // Then 마지막 프레임은 전부 보이고 팝인이 끝나 있다 — 이미지 저장물과 같은 장면이어야 한다
        assertEquals(4, frames.last().visibleCount)
        assertEquals(1f, frames.last().popProgress, 0f)
    }

    @Test
    fun toppingVideoFrames_firstFrameOfEachTopping_startsPopFromZero() {
        // When 토핑 2개
        val frames = toppingVideoFrames(toppingCount = 2)

        // Then 각 토핑의 첫 프레임에서 그 토핑이 막 나타난다
        assertEquals(1, frames[15].visibleCount)
        assertEquals(0f, frames[15].popProgress, 0f)
        assertEquals(2, frames[27].visibleCount)
        assertEquals(0f, frames[27].popProgress, 0f)
    }

    @Test
    fun toppingVideoFrames_holdFrames_keepPopFinished() {
        // When 토핑 1개
        val frames = toppingVideoFrames(toppingCount = 1)

        // Then 팝인 8프레임 뒤 유지 4프레임은 진행도가 1이다
        assertTrue(frames.subList(23, 27).all { it.popProgress == 1f })
    }

    @Test
    fun toppingPopScale_atStart_isZero() {
        assertEquals(0f, toppingPopScale(0f), 0.0001f)
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
    fun toppingPopAlpha_halfway_isAlreadyOpaque() {
        // Then 알파는 팝인 앞 절반에서 끝난다 — 크기가 자리 잡기 전에 색이 먼저 선다
        assertEquals(1f, toppingPopAlpha(0.5f), 0.0001f)
    }

    @Test
    fun toppingPopAlpha_atStart_isTransparent() {
        assertEquals(0f, toppingPopAlpha(0f), 0.0001f)
    }
}
