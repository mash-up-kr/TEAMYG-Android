package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasLoadStateTest {
    @Test
    fun canvasLoadState_nothingToLoad_isLoaded() {
        // Given 기다릴 것이 하나도 없다
        assertEquals(CanvasLoadState.Loaded, canvasLoadState(emptyList()))
    }

    @Test
    fun canvasLoadState_oneStillWaiting_isLoading() {
        assertEquals(
            CanvasLoadState.Loading,
            canvasLoadState(listOf(CanvasLoadState.Loaded, CanvasLoadState.Loading)),
        )
    }

    @Test
    fun canvasLoadState_everyOneLoaded_isLoaded() {
        assertEquals(
            CanvasLoadState.Loaded,
            canvasLoadState(listOf(CanvasLoadState.Loaded, CanvasLoadState.Loaded)),
        )
    }

    @Test
    fun canvasLoadState_oneFailed_isFailed() {
        assertEquals(
            CanvasLoadState.Failed,
            canvasLoadState(listOf(CanvasLoadState.Loaded, CanvasLoadState.Failed)),
        )
    }

    @Test
    fun canvasLoadState_failedWhileOthersWait_isFailedRightAway() {
        // Given 아직 오는 중인 것이 남았지만 이미 실패가 있다
        // Then 나머지를 다 기다린 뒤에야 실패를 말하면 그만큼 늦는다
        assertEquals(
            CanvasLoadState.Failed,
            canvasLoadState(listOf(CanvasLoadState.Loading, CanvasLoadState.Failed)),
        )
    }

    @Test
    fun canvasLoadState_backgroundFoldedWithToppings_isFailed() {
        // Given 토핑은 다 왔지만 배경을 못 받았다
        // Then 배경이 빠진 채로 저장하면 결과물이 틀린다
        val toppings = canvasLoadState(listOf(CanvasLoadState.Loaded, CanvasLoadState.Loaded))

        assertEquals(
            CanvasLoadState.Failed,
            canvasLoadState(listOf(CanvasLoadState.Failed, toppings)),
        )
    }

    @Test
    fun canvasLoadState_foldedTwice_keepsSameAnswer() {
        // Given 토핑을 먼저 접고 그 결과를 배경과 다시 접는다
        val toppings = canvasLoadState(listOf(CanvasLoadState.Loaded, CanvasLoadState.Loading))

        assertEquals(
            CanvasLoadState.Loading,
            canvasLoadState(listOf(CanvasLoadState.Loaded, toppings)),
        )
    }
}
