package com.teamyg.parfait.feature.camera.impl.viewmodel

import app.cash.turbine.test
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.model.FlashMode
import com.teamyg.parfait.feature.camera.impl.model.HalfZoomSource
import com.teamyg.parfait.feature.camera.impl.model.LensFacing
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CustomCameraViewModelTest {
    private fun createViewModel() = CustomCameraViewModel(
        createCameraCacheFileUseCase = mockk {
            every { this@mockk() } returns File("a.jpg")
        },
        createCameraCacheUriUseCase = mockk {
            every { this@mockk(file = any()) } returns "content://a"
        },
    )

    @Test
    fun permission_whenDeniedOnEntry_requestsSystemDialog() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionChecked(granted = false))

            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())
        }
    }

    @Test
    fun permission_whenStillDeniedAfterRequest_doesNotRequestAgain() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionChecked(granted = false))
            assertEquals(CustomCameraEffect.RequestPermission, awaitItem())

            // 다이얼로그가 닫힐 때마다 재개 확인이 들어오므로, 여기서 또 띄우면 끝없이 돈다
            viewModel.processIntent(
                CustomCameraIntent.OnPermissionRequestResult(granted = false),
            )
            viewModel.processIntent(CustomCameraIntent.OnPermissionChecked(granted = false))

            expectNoEvents()
        }
    }

    @Test
    fun permission_whenGrantedOnEntry_doesNotRequest() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnPermissionChecked(granted = true))

            expectNoEvents()
        }
    }

    @Test
    fun permission_whenGrantedAfterRequest_hasPermission() {
        val viewModel = createViewModel()

        viewModel.processIntent(CustomCameraIntent.OnPermissionRequestResult(granted = true))

        assertTrue(viewModel.state.value.hasPermission)
    }

    @Test
    fun flash_toggle_switchesOnAndOff() {
        val viewModel = createViewModel()

        viewModel.processIntent(CustomCameraIntent.OnClickFlash)
        assertEquals(FlashMode.ON, viewModel.state.value.flashMode)

        viewModel.processIntent(CustomCameraIntent.OnClickFlash)
        assertEquals(FlashMode.OFF, viewModel.state.value.flashMode)
    }

    @Test
    fun cancel_postsCancel() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnCancel)

            assertEquals(CustomCameraEffect.Cancel, awaitItem())
        }
    }

    @Test
    fun zoom_bothSupported_cyclesOneTwoHalf() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))

        assertEquals(
            listOf(CameraZoomLevel.TWO, CameraZoomLevel.HALF, CameraZoomLevel.ONE),
            viewModel.clickZoom(times = 3),
        )
    }

    @Test
    fun zoom_halfUnsupported_cyclesOneTwo() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = false))

        assertEquals(listOf(CameraZoomLevel.TWO, CameraZoomLevel.ONE), viewModel.clickZoom(times = 2))
    }

    @Test
    fun zoom_twoUnsupported_cyclesOneHalf() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..1.5f, hasUltraWideCamera = false))

        assertEquals(listOf(CameraZoomLevel.HALF, CameraZoomLevel.ONE), viewModel.clickZoom(times = 2))
    }

    @Test
    fun zoom_neitherSupported_hidesButton() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..1f, hasUltraWideCamera = false))

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
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))
        viewModel.clickZoom(times = 2)

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
    }

    @Test
    fun zoom_onNewRange_resetsToOneAndRebuildsLevels() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))
        viewModel.clickZoom(times = 1)

        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..1f, hasUltraWideCamera = false))

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.isZoomButtonVisible)
    }

    @Test
    fun zoom_ultraWideOnly_cyclesToHalfOnUltraWide() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = true))

        viewModel.clickZoom(times = 2)

        assertEquals(CameraZoomLevel.HALF, viewModel.state.value.zoomLevel)
        assertTrue(viewModel.state.value.shouldUseUltraWideCamera)
        assertEquals(1f, viewModel.state.value.cameraZoomRatio)

        viewModel.clickZoom(times = 1)

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.shouldUseUltraWideCamera)
    }

    @Test
    fun zoom_rangeCoversHalf_prefersZoomRatio() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = true))

        viewModel.clickZoom(times = 2)

        assertEquals(CameraZoomLevel.HALF, viewModel.state.value.zoomLevel)
        assertFalse(viewModel.state.value.shouldUseUltraWideCamera)
        assertEquals(0.5f, viewModel.state.value.cameraZoomRatio)
    }

    @Test
    fun zoom_onFlip_hidesButtonUntilNewRange() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))
        viewModel.clickZoom(times = 1)

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertFalse(viewModel.state.value.isZoomButtonVisible)
        assertEquals(HalfZoomSource.NONE, viewModel.state.value.halfZoomSource)
        assertEquals(listOf(CameraZoomLevel.ONE), viewModel.clickZoom(times = 1))
    }

    @Test
    fun zoom_rangeReportedAgain_keepsLevelIfStillAvailable() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))
        viewModel.clickZoom(times = 1)

        // 확인 화면에서 다시 찍기로 돌아오면 같은 카메라로 다시 바인딩되어 범위가 또 온다
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(0.5f..8f, hasUltraWideCamera = false))

        assertEquals(CameraZoomLevel.TWO, viewModel.state.value.zoomLevel)
    }

    @Test
    fun zoom_rangeReportedAgain_fallsBackToOneIfUnavailable() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = true))
        viewModel.clickZoom(times = 2)

        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = false))

        assertEquals(CameraZoomLevel.ONE, viewModel.state.value.zoomLevel)
    }

    @Test
    fun zoom_ultraWideWithoutTwo_cyclesOneHalf() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..1f, hasUltraWideCamera = true))

        assertEquals(listOf(CameraZoomLevel.HALF, CameraZoomLevel.ONE), viewModel.clickZoom(times = 2))
    }

    @Test
    fun zoom_flipFromUltraWide_unbindsUltraWide() {
        val viewModel = createViewModel()
        viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(1f..8f, hasUltraWideCamera = true))
        viewModel.clickZoom(times = 2)

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertFalse(viewModel.state.value.shouldUseUltraWideCamera)
    }

    private fun CustomCameraViewModel.clickZoom(times: Int): List<CameraZoomLevel> = List(times) {
        processIntent(CustomCameraIntent.OnClickZoom)
        state.value.zoomLevel
    }

    @Test
    fun flip_togglesLensFacing() {
        val viewModel = createViewModel()

        viewModel.processIntent(CustomCameraIntent.OnClickFlip)

        assertEquals(LensFacing.FRONT, viewModel.state.value.lensFacing)
    }

    @Test
    fun shutter_postsCaptureImage_andMarksCapturing() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)

            assertEquals(CustomCameraEffect.CaptureImage(File("a.jpg")), awaitItem())
            assertTrue(viewModel.state.value.isCapturing)
        }
    }

    @Test
    fun shutter_whileCapturing_ignored() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)

            assertEquals(CustomCameraEffect.CaptureImage(File("a.jpg")), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun captureSaved_clearsCapturing_andNavigates() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)
            awaitItem()

            viewModel.processIntent(CustomCameraIntent.OnCaptureSaved(File("a.jpg")))

            assertEquals(CustomCameraEffect.NavigateToConfirm("content://a"), awaitItem())
            assertFalse(viewModel.state.value.isCapturing)
        }
    }

    @Test
    fun captureFailed_clearsCapturing_andPostsFailed() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)
            awaitItem()

            viewModel.processIntent(CustomCameraIntent.OnCaptureFailed)

            assertEquals(CustomCameraEffect.CaptureFailed, awaitItem())
            assertFalse(viewModel.state.value.isCapturing)
        }
    }

    @Test
    fun captureCancelled_clearsCapturing_withoutEffect() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.processIntent(CustomCameraIntent.OnClickShutter)
            assertEquals(CustomCameraEffect.CaptureImage(File("a.jpg")), awaitItem())

            viewModel.processIntent(CustomCameraIntent.OnCaptureCancelled)

            assertFalse(viewModel.state.value.isCapturing)
            expectNoEvents()
        }
    }
}
