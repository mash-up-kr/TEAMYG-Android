package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasVideoFramePlanTest {
    @Test
    fun canvasVideoRevealSteps_returnsProgressFromFirstStepToOne() {
        // Given 토핑 하나가 4단계로 나타난다
        val steps = canvasVideoRevealSteps(stepsPerTopping = 4)

        // Then 0은 포함하지 않고(이미 이전 프레임에서 봤다) 1.0으로 끝난다
        assertEquals(listOf(0.25f, 0.5f, 0.75f, 1.0f), steps)
    }

    @Test
    fun canvasVideoRevealSteps_singleStep_returnsOnlyFullyRevealed() {
        val steps = canvasVideoRevealSteps(stepsPerTopping = 1)

        assertEquals(listOf(1.0f), steps)
    }
}
