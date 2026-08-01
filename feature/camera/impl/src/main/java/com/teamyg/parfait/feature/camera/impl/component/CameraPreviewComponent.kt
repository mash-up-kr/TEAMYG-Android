package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun CameraPreviewViewComponent(
    lensFacing: Int,
    zoomRatio: Float,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onZoomRangeReady: (ClosedFloatingPointRange<Float>) -> Unit,
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

    DisposableEffect(lensFacing) {
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
                .build()

            val selector: CameraSelector = CameraSelector
                .Builder()
                .requireLensFacing(lensFacing)
                .build()

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
            newCamera.cameraInfo.zoomState.value?.let { state ->
                onZoomRangeReady(state.minZoomRatio..state.maxZoomRatio)
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

    return CameraPreviewHandle(
        previewView = previewView,
        camera = cameraState,
    )
}
