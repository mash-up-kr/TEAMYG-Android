package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teamyg.parfait.feature.camera.impl.util.cameraSelectorOf
import com.teamyg.parfait.feature.camera.impl.util.findBackUltraWideCamera

@Composable
internal fun CameraPreviewViewComponent(
    lensFacing: Int,
    useUltraWideCamera: Boolean,
    zoomRatio: Float,
    hasPermission: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onZoomRangeReady: (range: ClosedFloatingPointRange<Float>, hasUltraWideCamera: Boolean) -> Unit,
): CameraPreviewHandle {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        // GraphicsLayer로 피드를 기록하려면 SurfaceView가 아닌 TextureView여야 한다.
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val cameraState = remember { mutableStateOf<Camera?>(null) }
    var camera by cameraState

    DisposableEffect(lensFacing, useUltraWideCamera, hasPermission) {
        // 권한 없이 바인딩하면 CameraX가 열기를 포기하고, 다이얼로그 허용은 lifecycle을 멈추지 않아 다시 열지 않는다
        if (!hasPermission) {
            return@DisposableEffect onDispose { }
        }

        var boundProvider: ProcessCameraProvider? = null
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = providerFuture.get()
            boundProvider = cameraProvider

            val preview: Preview = Preview
                .Builder()
                .build()
                .apply {
                    surfaceProvider = previewView.surfaceProvider
                }

            val imageCapture: ImageCapture = ImageCapture
                .Builder()
                // 근거: parfait/specs/2026-08-23-segmentation-preprocessing.md 「1. 손실이 처음 생기는 자리」
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(MAX_JPEG_QUALITY)
                .build()

            val ultraWide = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                cameraProvider.findBackUltraWideCamera()
            } else {
                null
            }
            val bindsUltraWide = useUltraWideCamera && ultraWide != null

            val selector: CameraSelector = if (bindsUltraWide) {
                cameraSelectorOf(cameraId = ultraWide.cameraId)
            } else {
                CameraSelector
                    .Builder()
                    .requireLensFacing(lensFacing)
                    .build()
            }

            cameraProvider.unbindAll()

            val newCamera = runCatching {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    imageCapture,
                )
            }.getOrNull() ?: return@Runnable

            camera = newCamera
            onImageCaptureReady(imageCapture)
            // 범위는 메인 카메라 것만 보고한다. 초광각 범위가 들어가면 VM이 1x로 초기화해 0.5x가 풀린다
            if (!bindsUltraWide) {
                newCamera.cameraInfo.zoomState.value?.let { state ->
                    onZoomRangeReady(state.minZoomRatio..state.maxZoomRatio, ultraWide != null)
                }
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            boundProvider?.unbindAll()
            camera = null
        }
    }

    LaunchedEffect(zoomRatio, camera) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    // CameraX는 카메라가 닫히면(ON_STOP) 줌을 1.0으로 되돌리고, 복귀 때는 위 키가 그대로라 다시 걸지 않는다
    val currentZoomRatio by rememberUpdatedState(zoomRatio)
    DisposableEffect(camera) {
        val target = camera ?: return@DisposableEffect onDispose { }
        val observer = Observer<CameraState> { state ->
            if (state.type == CameraState.Type.OPEN) {
                target.cameraControl.setZoomRatio(currentZoomRatio)
            }
        }
        target.cameraInfo.cameraState.observeForever(observer)

        onDispose { target.cameraInfo.cameraState.removeObserver(observer) }
    }

    return CameraPreviewHandle(
        previewView = previewView,
        camera = cameraState,
    )
}

private const val MAX_JPEG_QUALITY = 100
