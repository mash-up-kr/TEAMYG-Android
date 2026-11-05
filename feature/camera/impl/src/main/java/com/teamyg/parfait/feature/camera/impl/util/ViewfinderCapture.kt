package com.teamyg.parfait.feature.camera.impl.util

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 촬영해 뷰파인더에 보이던 영역만 [file]에 저장한다. 디코드·크롭·저장은 IO 스레드에서 돈다.
 *
 * 크롭 영역을 계산할 수 없으면 피드에 보이던 영역을, 그것도 없으면 전체 프레임을 저장한다.
 *
 * @param isViewPortApplied 촬영한 [ImageCapture]가 ViewPort 와 함께 바인딩됐는지. 그래야 `cropRect`가 피드에 보이는 영역이다.
 * @throws Exception 촬영·디코드·저장 중 하나라도 실패하면 그 예외를 그대로 던진다.
 */
internal suspend fun ImageCapture.captureViewfinder(
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
                val saved = runCatching {
                    image.saveViewfinderRegion(
                        file = file,
                        viewfinderRect = viewfinderRect,
                        feedRect = feedRect,
                        isFrontFacing = isFrontFacing,
                        isViewPortApplied = isViewPortApplied,
                    )
                }
                saved
                    .onSuccess { continuation.resume(Unit) }
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
    // toBitmap() 은 JPEG 에서 cropRect 를 적용하지 않고 버퍼 전체를 디코드한다. 그래서 cropRect 는 버퍼 좌표로 직접 쓴다
    val (rotationDegrees, bufferCropRect, captured) = use { image ->
        Triple(image.imageInfo.rotationDegrees, image.cropRect, image.toBitmap())
    }

    val visibleRect = if (isViewPortApplied) {
        IntRect(bufferCropRect.left, bufferCropRect.top, bufferCropRect.right, bufferCropRect.bottom)
    } else {
        feedRect?.let {
            fallbackCropRect(
                bufferSize = IntSize(captured.width, captured.height),
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

    saveCroppedCapture(
        captured = captured,
        cropRect = cropRect,
        rotationDegrees = rotationDegrees,
        file = file,
    )
}
