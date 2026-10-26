package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasRevealSettledTest {
    @Test
    fun textsOnly_areAllSettled() {
        // Given·When 이미지 없이 텍스트만 둘
        // Then 기다릴 것이 없어 일괄 등장이 막히지 않는다
        assertEquals(listOf(true, true), canvasRevealSettled(imageStates = emptyList(), textCount = 2))
    }

    @Test
    fun loadingImageWithText_isNotAllSettled() {
        // Given·When 이미지 하나가 아직 받는 중이고 텍스트가 하나 있다
        val settled = canvasRevealSettled(imageStates = listOf(CanvasLoadState.Loading), textCount = 1)

        // Then 이미지를 기다린다
        assertEquals(listOf(false, true), settled)
    }

    @Test
    fun failedImage_countsAsSettled() {
        // Given·When 실패도 결말이다
        val settled = canvasRevealSettled(imageStates = listOf(CanvasLoadState.Failed), textCount = 0)

        // Then
        assertEquals(listOf(true), settled)
    }

    @Test
    fun empty_isEmpty() {
        // Given·When 아무것도 없다 — 빈 목록은 빗장이 바로 풀리는 기존 동작을 따른다
        // Then
        assertEquals(emptyList(), canvasRevealSettled(imageStates = emptyList(), textCount = 0))
    }
}
