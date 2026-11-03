package com.teamyg.parfait.feature.camera.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.model.HalfZoomSource
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomCameraViewModelTest {
    private fun createViewModel() = CustomCameraViewModel(
        createCameraCacheFileUseCase = mockk(),
        createCameraCacheUriUseCase = mockk(),
    )

    @Test
    fun permission_whenDeniedOnEntry_requestsSystemDialog() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))

            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())
        }
    }

    @Test
    fun permission_whenStillDeniedAfterRequest_doesNotRequestAgain() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))
            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())

            // 다이얼로그가 닫힐 때마다 재개 확인이 들어오므로, 여기서 또 띄우면 끝없이 돈다
            viewModel.processIntent(
                CustomCameraIntent.OnPermissionRequestResult(granted = false, shouldShowRationale = false),
            )
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = false))

            expectNoEvents()
        }
    }

    @Test
    fun permission_whenGrantedOnEntry_doesNotRequest() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted = true))

            expectNoEvents()
        }
    }

    @Test
    fun zoom_bothSupported_cyclesOneTwoHalf() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f))

        assertEquals(
            listOf(CameraZoomLevel.TWO, CameraZoomLevel.HALF, CameraZoomLevel.ONE),
            viewModel.clickZoom(times = 3),
        )
    }

    @Test
    fun zoom_halfUnsupported_cyclesOneTwo() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f))

        assertEquals(listOf(CameraZoomLevel.TWO, CameraZoomLevel.ONE), viewModel.clickZoom(times = 2))
    }

    @Test
    fun zoom_twoUnsupported_cyclesOneHalf() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..1.5f))

        assertEquals(listOf(CameraZoomLevel.HALF, CameraZoomLevel.ONE), viewModel.clickZoom(times = 2))
    }

    @Test
    fun zoom_neitherSupported_hidesButton() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..1f))

        assertFalse(viewModel.state.value.isZoomButtonVisible)
        assertEquals(listOf(CameraZoomLevel.ONE), viewModel.clickZoom(times = 1))
    }

    @Test
    fun zoom_beforeRangeReady_hidesButton() {
        val viewModel = createViewModel()

        assertFalse(viewModel.state.value.isZoomButtonVisible)
        assertEquals(listOf(CameraZoomLevel.ONE), viewModel.clickZoom(times = 1))
    }

    @Test
    fun zoom_onFlip_resetsToOne() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f))
        viewModel.clickZoom(times = 2)

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
    }

    @Test
    fun zoom_onNewRange_resetsToOneAndRebuildsLevels() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f))
        viewModel.clickZoom(times = 1)

        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..1f))

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.isZoomButtonVisible)
    }

    @Test
    fun zoom_ultraWideOnly_cyclesToHalfOnUltraWide() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = true))

        viewModel.clickZoom(times = 2)

        assertEquals(CameraZoomLevel.HALF, viewModel.state.value.zoomLevel)
        assertTrue(viewModel.state.value.useUltraWideCamera)
        assertEquals(1f, viewModel.state.value.cameraZoomRatio)

        viewModel.clickZoom(times = 1)

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.useUltraWideCamera)
    }

    @Test
    fun zoom_rangeCoversHalf_prefersZoomRatio() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = true))

        viewModel.clickZoom(times = 2)

        assertEquals(CameraZoomLevel.HALF, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.useUltraWideCamera)
        assertEquals(0.5f, viewModel.state.value.cameraZoomRatio)
    }

    @Test
    fun zoom_onFlip_hidesButtonUntilNewRange() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f))
        viewModel.clickZoom(times = 1)

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertFalse(viewModel.state.value.isZoomButtonVisible)
        assertEquals(HalfZoomSource.NONE, viewModel.state.value.halfZoomSource)
        assertEquals(listOf(CameraZoomLevel.ONE), viewModel.clickZoom(times = 1))
    }

    private fun CustomCameraViewModel.clickZoom(times: Int): List<CameraZoomLevel> = List(times) {
        processIntent(CustomCameraIntent.OnClickZoom)
        state.value.zoomLevel
    }
}
