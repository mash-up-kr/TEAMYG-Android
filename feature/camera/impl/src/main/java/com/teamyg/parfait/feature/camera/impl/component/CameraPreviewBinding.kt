package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teamyg.parfait.feature.camera.impl.model.CameraPreviewState
import com.teamyg.parfait.feature.camera.impl.model.CameraTransition
import com.teamyg.parfait.feature.camera.impl.model.CapturedFrame
import com.teamyg.parfait.feature.camera.impl.model.LensFacing
import com.teamyg.parfait.feature.camera.impl.util.cameraSelectorOf
import com.teamyg.parfait.feature.camera.impl.util.findBackUltraWideCamera

@Composable
internal fun rememberCameraPreviewState(): CameraPreviewState {
    val context = LocalContext.current
    return remember {
        // GraphicsLayer로 피드를 기록하려면 SurfaceView가 아닌 TextureView여야 한다.
        val previewView = PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
        CameraPreviewState(previewView = previewView)
    }
}

@Composable
internal fun CameraPreviewBindingEffect(
    state: CameraPreviewState,
    lensFacing: LensFacing,
    shouldUseUltraWideCamera: Boolean,
    hasPermission: Boolean,
    onZoomRangeReady: (range: ClosedFloatingPointRange<Float>, hasUltraWideCamera: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnZoomRangeReady by rememberUpdatedState(onZoomRangeReady)

    DisposableEffect(state, lensFacing, shouldUseUltraWideCamera, hasPermission) {
        // 권한 없이 바인딩하면 CameraX가 열기를 포기하고, 다이얼로그 허용은 lifecycle을 멈추지 않아 다시 열지 않는다
        if (!hasPermission) {
            state.lastFrame = null
            state.transition = null
            state.imageCapture = null
            return@DisposableEffect onDispose { }
        }

        state.transition = state.lastFrame?.let { captured ->
            CameraTransition(frame = captured.bitmap.asImageBitmap(), isFlip = captured.lensFacing != lensFacing)
        }
        state.lastFrame = null

        var boundProvider: ProcessCameraProvider? = null
        // 리스너는 비동기로 돈다. 폐기된 바인딩이 뒤늦게 돌아 새 바인딩을 덮지 않게 한다
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (disposed) return@Runnable
            val cameraProvider = providerFuture.get()
            boundProvider = cameraProvider

            val preview: Preview = Preview
                .Builder()
                .build()
                .apply {
                    surfaceProvider = state.previewView.surfaceProvider
                }

            val imageCapture: ImageCapture = ImageCapture
                .Builder()
                // 근거: parfait/specs/2026-08-23-segmentation-preprocessing.md 「1. 손실이 처음 생기는 자리」
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(MAX_JPEG_QUALITY)
                .build()

            val ultraWide = if (lensFacing == LensFacing.BACK) {
                cameraProvider.findBackUltraWideCamera()?.takeIf { it.cameraId !in state.failedUltraWideIds }
            } else {
                null
            }
            val shouldBindUltraWide = shouldUseUltraWideCamera && ultraWide != null

            // 뷰가 아직 크기·디스플레이를 못 받았으면 null 이다. 그때는 ViewPort 없이 바인딩한다
            val viewPort: ViewPort? = state.previewView.viewPort

            val lensSelector: CameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing.cameraSelectorLens)
                .build()

            val ultraWideCamera = if (shouldBindUltraWide) {
                cameraProvider.bindUseCases(
                    lifecycleOwner = lifecycleOwner,
                    selector = cameraSelectorOf(cameraId = ultraWide.cameraId),
                    preview = preview,
                    imageCapture = imageCapture,
                    viewPort = viewPort,
                )
            } else {
                null
            }
            val isUltraWideFailed = shouldBindUltraWide && ultraWideCamera == null
            if (isUltraWideFailed) state.failedUltraWideIds += ultraWide.cameraId
            val newCamera = ultraWideCamera ?: cameraProvider.bindUseCases(
                lifecycleOwner = lifecycleOwner,
                selector = lensSelector,
                preview = preview,
                imageCapture = imageCapture,
                viewPort = viewPort,
            )
            state.isViewPortApplied = newCamera != null && viewPort != null
            if (newCamera == null) {
                // 새 스트림이 오지 않으니 덮개를 바로 걷는다
                state.transition = null
                return@Runnable
            }

            state.camera = newCamera
            state.imageCapture = imageCapture
            // 범위는 메인 카메라 것만 보고한다. 초광각 범위로 단계 목록을 만들면 2x·0.5x 판정이 틀어진다
            if (ultraWideCamera == null) {
                newCamera.cameraInfo.zoomState.value?.let { zoomState ->
                    currentOnZoomRangeReady(
                        zoomState.minZoomRatio..zoomState.maxZoomRatio,
                        ultraWide != null && !isUltraWideFailed,
                    )
                }
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            state.lastFrame = state.previewView.bitmap?.let { CapturedFrame(bitmap = it, lensFacing = lensFacing) }
            boundProvider?.unbindAll()
            state.camera = null
            state.imageCapture = null
        }
    }
}

private fun ProcessCameraProvider.bindUseCases(
    lifecycleOwner: LifecycleOwner,
    selector: CameraSelector,
    preview: Preview,
    imageCapture: ImageCapture,
    viewPort: ViewPort?,
): Camera? {
    unbindAll()
    return runCatching {
        if (viewPort != null) {
            val useCaseGroup = UseCaseGroup
                .Builder()
                .setViewPort(viewPort)
                .addUseCase(preview)
                .addUseCase(imageCapture)
                .build()
            bindToLifecycle(lifecycleOwner, selector, useCaseGroup)
        } else {
            bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture,
            )
        }
    }.getOrNull()
}

private const val MAX_JPEG_QUALITY = 100
