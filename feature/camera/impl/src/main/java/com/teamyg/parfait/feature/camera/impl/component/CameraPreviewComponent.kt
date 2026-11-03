package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teamyg.parfait.feature.camera.impl.model.AppliedZoom
import com.teamyg.parfait.feature.camera.impl.model.CameraTransition
import com.teamyg.parfait.feature.camera.impl.model.CapturedFrame
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
    val transitionState = remember { mutableStateOf<CameraTransition?>(null) }
    // 직전 바인딩이 dispose 때 남긴 프레임. 다음 바인딩이 덮개로 쓴다
    val lastFrame = remember { mutableStateOf<CapturedFrame?>(null) }
    // 한 번 안 열린 초광각은 다시 쓰지 않는다. 안 막으면 메인 재바인딩에서 0.5x가 되살아난다
    val failedUltraWideIds = remember { mutableSetOf<String>() }

    DisposableEffect(lensFacing, useUltraWideCamera, hasPermission) {
        // 권한 없이 바인딩하면 CameraX가 열기를 포기하고, 다이얼로그 허용은 lifecycle을 멈추지 않아 다시 열지 않는다
        if (!hasPermission) {
            lastFrame.value = null
            transitionState.value = null
            return@DisposableEffect onDispose { }
        }

        transitionState.value = lastFrame.value?.let { captured ->
            CameraTransition(frame = captured.bitmap.asImageBitmap(), isFlip = captured.lensFacing != lensFacing)
        }
        lastFrame.value = null

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
                    surfaceProvider = previewView.surfaceProvider
                }

            val imageCapture: ImageCapture = ImageCapture
                .Builder()
                // 근거: parfait/specs/2026-08-23-segmentation-preprocessing.md 「1. 손실이 처음 생기는 자리」
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(MAX_JPEG_QUALITY)
                .build()

            val ultraWide = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                cameraProvider.findBackUltraWideCamera()?.takeIf { it.cameraId !in failedUltraWideIds }
            } else {
                null
            }
            val bindsUltraWide = useUltraWideCamera && ultraWide != null

            val lensSelector: CameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

            fun bind(selector: CameraSelector): Camera? {
                cameraProvider.unbindAll()
                return runCatching {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        preview,
                        imageCapture,
                    )
                }.getOrNull()
            }

            val ultraWideCamera = if (bindsUltraWide) bind(cameraSelectorOf(cameraId = ultraWide.cameraId)) else null
            val ultraWideFailed = bindsUltraWide && ultraWideCamera == null
            if (ultraWideFailed) failedUltraWideIds += ultraWide.cameraId
            val newCamera = ultraWideCamera ?: bind(lensSelector)
            if (newCamera == null) {
                // 새 스트림이 오지 않으니 덮개를 바로 걷는다
                transitionState.value = null
                return@Runnable
            }

            camera = newCamera
            onImageCaptureReady(imageCapture)
            // 범위는 메인 카메라 것만 보고한다. 초광각 범위로 단계 목록을 만들면 2x·0.5x 판정이 틀어진다
            if (ultraWideCamera == null) {
                newCamera.cameraInfo.zoomState.value?.let { state ->
                    onZoomRangeReady(state.minZoomRatio..state.maxZoomRatio, ultraWide != null && !ultraWideFailed)
                }
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            disposed = true
            lastFrame.value = previewView.bitmap?.let { CapturedFrame(bitmap = it, lensFacing = lensFacing) }
            boundProvider?.unbindAll()
            camera = null
        }
    }

    val appliedZoom = remember { AppliedZoom() }
    LaunchedEffect(zoomRatio, camera) {
        val target = camera ?: return@LaunchedEffect
        if (target !== appliedZoom.camera) {
            appliedZoom.camera = target
            appliedZoom.ratio = zoomRatio
            target.cameraControl.setZoomRatio(zoomRatio)
            return@LaunchedEffect
        }

        animate(
            initialValue = appliedZoom.ratio,
            targetValue = zoomRatio,
            animationSpec = tween(durationMillis = ZOOM_ANIMATION_MILLIS),
        ) { value, _ ->
            appliedZoom.ratio = value
            target.cameraControl.setZoomRatio(value)
        }
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

    // 덮개를 건 직후엔 이전 스트림의 STREAMING이 남아 있을 수 있어 IDLE을 본 뒤의 STREAMING만 센다
    val hasTransition = transitionState.value != null
    DisposableEffect(hasTransition) {
        if (!hasTransition) return@DisposableEffect onDispose { }

        var sawIdle = false
        val observer = Observer<PreviewView.StreamState> { streamState ->
            when (streamState) {
                PreviewView.StreamState.IDLE -> sawIdle = true
                PreviewView.StreamState.STREAMING -> if (sawIdle) transitionState.value = null
                else -> Unit
            }
        }
        previewView.previewStreamState.observeForever(observer)

        onDispose { previewView.previewStreamState.removeObserver(observer) }
    }

    // IDLE을 건너뛰는 기기에서도 덮개가 남지 않게 한다
    val transition = transitionState.value
    LaunchedEffect(transition) {
        if (transition == null) return@LaunchedEffect
        delay(TRANSITION_TIMEOUT_MILLIS)
        if (transitionState.value === transition) transitionState.value = null
    }

    return CameraPreviewHandle(
        previewView = previewView,
        camera = cameraState,
        transition = transitionState,
    )
}

private const val MAX_JPEG_QUALITY = 100
private const val ZOOM_ANIMATION_MILLIS = 200
private const val TRANSITION_TIMEOUT_MILLIS = 2_000L
