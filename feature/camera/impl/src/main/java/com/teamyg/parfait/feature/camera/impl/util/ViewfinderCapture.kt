package com.teamyg.parfait.feature.camera.impl.util

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.teamyg.parfait.feature.camera.impl.model.CameraPreviewState
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 촬영해 뷰파인더에 보이던 영역만 [file]에 저장한다. 디코드·크롭·저장은 IO 스레드에서 돈다.
 *
 * 크롭 영역을 계산할 수 없으면 피드에 보이던 영역을, 그것도 없으면 전체 프레임을 저장한다.
 */
internal suspend fun CameraPreviewState.captureViewfinder(
    file: File,
    viewfinderRect: Rect?,
    feedRect: Rect?,
    isFrontFacing: Boolean,
) {
    val capture = imageCapture ?: throw IllegalStateException("바인딩된 ImageCapture 가 없다")
    capture.captureViewfinder(
        file = file,
        viewfinderRect = viewfinderRect,
        feedRect = feedRect,
        isFrontFacing = isFrontFacing,
        isViewPortApplied = isViewPortApplied,
    )
}

/**
 * @param isViewPortApplied 촬영한 [ImageCapture]가 ViewPort 와 함께 바인딩됐는지. 그래야 `cropRect`가 피드에 보이는 영역이다.
 */
private suspend fun ImageCapture.captureViewfinder(
    file: File,
    viewfinderRect: Rect?,
    feedRect: Rect?,
    isFrontFacing: Boolean,
    isViewPortApplied: Boolean,
): Unit = suspendCancellableCoroutine { continuation ->
    takePicture(
        Dispatchers.IO.asExecutor(),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                runCatching {
                    image.saveViewfinderRegion(
                        file = file,
                        viewfinderRect = viewfinderRect,
                        feedRect = feedRect,
                        isFrontFacing = isFrontFacing,
                        isViewPortApplied = isViewPortApplied,
                    )
                }.onSuccess { continuation.resume(Unit) }
                    .onFailure { continuation.resumeWithException(it) }
            }

            override fun onError(exception: ImageCaptureException) {
                continuation.resumeWithException(exception)
            }
        },
    )
}

private fun ImageProxy.saveViewfinderRegion(
    file: File,
    viewfinderRect: Rect?,
    feedRect: Rect?,
    isFrontFacing: Boolean,
    isViewPortApplied: Boolean,
) {
    // JPEG 는 바이트만 복사해 버퍼를 바로 돌려주고, 크롭 영역만 디코드한다. 그 밖의 포맷은 전체를 디코드한다
    val captured = use { image ->
        CapturedImage(
            rotationDegrees = image.imageInfo.rotationDegrees,
            bufferSize = IntSize(image.width, image.height),
            bufferCropRect = image.cropRect.let { IntRect(it.left, it.top, it.right, it.bottom) },
            pixels = if (image.format == ImageFormat.JPEG) {
                CapturedPixels.Jpeg(image.planes[0].buffer.toByteArray())
            } else {
                CapturedPixels.Decoded(image.toBitmap())
            },
        )
    }
    val rotationDegrees = captured.rotationDegrees

    // ImageProxy.cropRect 는 toBitmap()·디코드 어느 쪽에도 적용되지 않으므로 버퍼 좌표로 직접 쓴다
    val visibleRect = if (isViewPortApplied) {
        captured.bufferCropRect
    } else {
        feedRect?.let {
            fallbackCropRect(
                bufferSize = captured.bufferSize,
                rotationDegrees = rotationDegrees,
                feedSize = it.size,
            )
        }
    }
    val cropRect = if (viewfinderRect != null && feedRect != null && visibleRect != null) {
        computeCaptureCropRect(
            viewfinderRect = viewfinderRect,
            feedRect = feedRect,
            visibleRect = visibleRect,
            rotationDegrees = rotationDegrees,
            isFrontFacing = isFrontFacing,
        ) ?: visibleRect
    } else {
        visibleRect
    }

    when (val pixels = captured.pixels) {
        is CapturedPixels.Jpeg -> saveCroppedJpeg(
            jpeg = pixels.bytes,
            cropRect = cropRect,
            rotationDegrees = rotationDegrees,
            file = file,
        )

        is CapturedPixels.Decoded -> saveCroppedCapture(
            captured = pixels.bitmap,
            cropRect = cropRect,
            rotationDegrees = rotationDegrees,
            file = file,
        )
    }
}

private fun ByteBuffer.toByteArray(): ByteArray {
    rewind()
    return ByteArray(remaining()).also { get(it) }
}

private class CapturedImage(
    val rotationDegrees: Int,
    val bufferSize: IntSize,
    val bufferCropRect: IntRect,
    val pixels: CapturedPixels,
)

private sealed interface CapturedPixels {
    class Jpeg(
        val bytes: ByteArray,
    ) : CapturedPixels

    class Decoded(
        val bitmap: Bitmap,
    ) : CapturedPixels
}
