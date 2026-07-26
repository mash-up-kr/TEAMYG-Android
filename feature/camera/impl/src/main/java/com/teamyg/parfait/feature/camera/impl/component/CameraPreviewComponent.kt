package com.teamyg.parfait.feature.camera.impl.component

import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

internal class CameraPreviewViews(
    val backgroundPreviewView: PreviewView,
    val foregroundPreviewView: PreviewView,
    val camera: State<Camera?>,
)

@Composable
internal fun CameraPreviewViewComponent(
    lensFacing: Int,
    zoomRatio: Float,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onZoomRangeReady: (ClosedFloatingPointRange<Float>) -> Unit,
): CameraPreviewViews {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backgroundPreviewView = remember { PreviewView(context) }
    val foregroundPreviewView = remember { PreviewView(context) }
    val cameraState = remember { mutableStateOf<Camera?>(null) }
    var camera by cameraState

    DisposableEffect(lensFacing) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            val cameraProvider = providerFuture.get()

            val backgroundPreview: Preview = Preview
                .Builder()
                .build()
                .apply {
                    surfaceProvider = backgroundPreviewView.surfaceProvider
                }

            val foregroundPreview: Preview = Preview
                .Builder()
                .build()
                .apply {
                    surfaceProvider = foregroundPreviewView.surfaceProvider
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
                lifecycleOwner,
                selector,
                backgroundPreview,
                foregroundPreview,
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

    return CameraPreviewViews(
        backgroundPreviewView = backgroundPreviewView,
        foregroundPreviewView = foregroundPreviewView,
        camera = cameraState,
    )
}

@Composable
internal fun CameraBackgroundPreview(
    previewView: PreviewView,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YGAtomicColors.Transparency.Black25),
        )
    }
}

@Composable
internal fun CameraViewfinderPreview(
    previewView: PreviewView,
    camera: Camera?,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { previewView },
        modifier = modifier
            .onSizeChanged { size ->
                previewView.layoutParams = ViewGroup.LayoutParams(size.width, size.height)
            }
            .pointerInput(camera) {
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
