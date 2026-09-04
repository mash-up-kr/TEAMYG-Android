package com.teamyg.parfait.feature.groups.canvas.impl.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CanvasToppingLoadStateTest {
    @Test
    fun canvasToppingLoadState_nothingToLoad_isLoaded() {
        // Given 토핑이 하나도 없는 빈 캔버스
        // Then 기다릴 대상이 없으니 실패도 아니다
        assertEquals(CanvasToppingLoadState.Loaded, canvasToppingLoadState(emptyList()))
    }

    @Test
    fun canvasToppingLoadState_stillWaiting_isLoading() {
        assertEquals(
            CanvasToppingLoadState.Loading,
            canvasToppingLoadState(listOf(ToppingImageState.Loaded, ToppingImageState.Loading)),
        )
    }

    @Test
    fun canvasToppingLoadState_everyOneLoaded_isLoaded() {
        assertEquals(
            CanvasToppingLoadState.Loaded,
            canvasToppingLoadState(listOf(ToppingImageState.Loaded, ToppingImageState.Loaded)),
        )
    }

    @Test
    fun canvasToppingLoadState_oneFailed_isFailed() {
        // Given 한 장만 못 받았다
        // Then 캔버스가 온전하지 않으므로 다시 시도를 권한다
        assertEquals(
            CanvasToppingLoadState.Failed,
            canvasToppingLoadState(listOf(ToppingImageState.Loaded, ToppingImageState.Failed)),
        )
    }

    @Test
    fun canvasToppingLoadState_failedWhileOthersWait_isFailedRightAway() {
        // Given 아직 오는 중인 것이 남았지만 이미 실패가 있다
        // Then 나머지를 다 기다린 뒤에야 실패를 말하면 그만큼 늦는다
        assertEquals(
            CanvasToppingLoadState.Failed,
            canvasToppingLoadState(listOf(ToppingImageState.Loading, ToppingImageState.Failed)),
        )
    }
}
