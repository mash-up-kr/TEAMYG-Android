package com.teamyg.parfait.feature.camera.impl.component

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
internal fun CameraPreviewComponent(
    lensFacing: Int,
    zoomRatio: Float,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onZoomRangeReady: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(lensFacing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = providerFuture.get()
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

            val newCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner = lifecycleOwner,
                cameraSelector = selector,
                preview,
                imageCapture,
            )
            camera = newCamera
            onImageCaptureReady(imageCapture)
            newCamera.cameraInfo.zoomState.value?.let { state ->
                onZoomRangeReady(state.minZoomRatio..state.maxZoomRatio)
            }
        }

        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            providerFuture.get().unbindAll()
            camera = null
        }
    }

    LaunchedEffect(zoomRatio, camera) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.pointerInput(camera) {
            detectTapGestures { offset ->
                val cam = camera ?: return@detectTapGestures

                val point = previewView.meteringPointFactory
                    .createPoint(offset.x, offset.y)

                val action: FocusMeteringAction = FocusMeteringAction
                    .Builder(point)
                    .build()

                cam.cameraControl.startFocusAndMetering(action)
            }
        },
    )
}
