package com.teamyg.parfait.feature.camera.impl.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 화면의 뷰파인더 영역을 촬영 이미지의 픽셀 영역으로 옮긴다.
 *
 * PreviewView 는 FILL_CENTER 로 이미지를 피드 영역에 꽉 채워(넘치는 부분은 잘라) 그리므로,
 * 같은 배율·중심 정렬을 역으로 적용하면 화면 좌표가 이미지 좌표로 환산된다.
 *
 * @param viewfinderRect 뷰파인더 영역(루트 좌표, px)
 * @param feedRect 카메라 피드가 그려지는 영역(루트 좌표, px)
 * @param imageSize 회전 보정을 마친 촬영 이미지 크기
 * @param isFrontFacing 전면 카메라 여부. 프리뷰만 좌우 반전되므로 이미지 좌표에서 되돌린다.
 * @return 촬영 이미지에서 잘라낼 영역. 계산이 불가능하면 null.
 */
internal fun computeCropRect(
    viewfinderRect: Rect,
    feedRect: Rect,
    imageSize: IntSize,
    isFrontFacing: Boolean,
): IntRect? {
    if (feedRect.width <= 0f || feedRect.height <= 0f) return null
    if (imageSize.width <= 0 || imageSize.height <= 0) return null

    val scale = max(
        feedRect.width / imageSize.width,
        feedRect.height / imageSize.height,
    )

    // 이미지가 피드 영역 안에서 차지하는 사각형의 좌상단(중앙 정렬이라 음수일 수 있다)
    val imageOriginX = feedRect.left + (feedRect.width - imageSize.width * scale) / 2f
    val imageOriginY = feedRect.top + (feedRect.height - imageSize.height * scale) / 2f

    val left = ((viewfinderRect.left - imageOriginX) / scale).roundToInt()
    val top = ((viewfinderRect.top - imageOriginY) / scale).roundToInt()
    val right = ((viewfinderRect.right - imageOriginX) / scale).roundToInt()
    val bottom = ((viewfinderRect.bottom - imageOriginY) / scale).roundToInt()

    val clampedLeft = left.coerceIn(0, imageSize.width)
    val clampedTop = top.coerceIn(0, imageSize.height)
    val clampedRight = right.coerceIn(0, imageSize.width)
    val clampedBottom = bottom.coerceIn(0, imageSize.height)

    if (clampedRight <= clampedLeft || clampedBottom <= clampedTop) return null

    return if (isFrontFacing) {
        IntRect(
            left = imageSize.width - clampedRight,
            top = clampedTop,
            right = imageSize.width - clampedLeft,
            bottom = clampedBottom,
        )
    } else {
        IntRect(
            left = clampedLeft,
            top = clampedTop,
            right = clampedRight,
            bottom = clampedBottom,
        )
    }
}

/** 촬영 이미지는 센서 방향 그대로 오므로 표시 방향에 맞춰 회전시킨다. */
internal fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/** 계산된 영역으로 잘라낸다. 영역이 없으면 원본을 그대로 돌려준다. */
internal fun Bitmap.crop(rect: IntRect?): Bitmap {
    if (rect == null) return this

    return Bitmap.createBitmap(this, rect.left, rect.top, rect.width, rect.height)
}

/**
 * 촬영 이미지를 뷰파인더에 보이던 영역으로 잘라 파일에 저장한다.
 * 영역을 계산할 수 없으면 잘라내지 않고 전체 프레임을 저장한다.
 */
internal fun saveViewfinderCapture(
    captured: Bitmap,
    rotationDegrees: Int,
    viewfinderRect: Rect?,
    feedRect: Rect?,
    isFrontFacing: Boolean,
    file: File,
) {
    val rotated = captured.rotate(rotationDegrees)
    val cropRect = if (viewfinderRect != null && feedRect != null) {
        computeCropRect(
            viewfinderRect = viewfinderRect,
            feedRect = feedRect,
            imageSize = IntSize(rotated.width, rotated.height),
            isFrontFacing = isFrontFacing,
        )
    } else {
        null
    }

    file.outputStream().use { output ->
        rotated.crop(cropRect).compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    }
}

/**
 * 뷰파인더 영역을 ViewPort 로 잘린 피드 기준으로 환산해 촬영 버퍼의 픽셀 영역으로 옮긴다.
 * `visibleRect`와 반환값은 버퍼(회전 전) 좌표다.
 *
 * @return 계산이 불가능하거나 영역이 비면 null.
 */
internal fun computeCaptureCropRect(
    viewfinderRect: Rect,
    feedRect: Rect,
    visibleRect: IntRect,
    rotationDegrees: Int,
    isFrontFacing: Boolean,
): IntRect? {
    if (feedRect.width <= 0f || feedRect.height <= 0f) return null
    if (visibleRect.width <= 0 || visibleRect.height <= 0) return null

    val uStart = ((viewfinderRect.left - feedRect.left) / feedRect.width).coerceIn(0f, 1f)
    val uEnd = ((viewfinderRect.right - feedRect.left) / feedRect.width).coerceIn(0f, 1f)
    val vStart = ((viewfinderRect.top - feedRect.top) / feedRect.height).coerceIn(0f, 1f)
    val vEnd = ((viewfinderRect.bottom - feedRect.top) / feedRect.height).coerceIn(0f, 1f)

    val display = if (isFrontFacing) {
        Rect(1f - uEnd, vStart, 1f - uStart, vEnd)
    } else {
        Rect(uStart, vStart, uEnd, vEnd)
    }
    val buffer = displayToBufferFraction(display, rotationDegrees)

    val left = visibleRect.left + (buffer.left * visibleRect.width).roundToInt()
    val top = visibleRect.top + (buffer.top * visibleRect.height).roundToInt()
    val right = visibleRect.left + (buffer.right * visibleRect.width).roundToInt()
    val bottom = visibleRect.top + (buffer.bottom * visibleRect.height).roundToInt()

    if (right <= left || bottom <= top) return null
    return IntRect(left, top, right, bottom)
}

/** ViewPort 없이 바인딩됐을 때 FILL_CENTER 가 보여 주는 중앙 영역을 버퍼(회전 전) 좌표로 돌려준다. */
internal fun fallbackCropRect(
    bufferSize: IntSize,
    rotationDegrees: Int,
    feedSize: Size,
): IntRect {
    val isQuarterTurn = rotationDegrees % 180 != 0
    val displayWidth = (if (isQuarterTurn) bufferSize.height else bufferSize.width).toFloat()
    val displayHeight = (if (isQuarterTurn) bufferSize.width else bufferSize.height).toFloat()

    val feedAspect = feedSize.width / feedSize.height
    val visibleWidth = min(displayWidth, displayHeight * feedAspect)
    val visibleHeight = min(displayHeight, displayWidth / feedAspect)

    val display = Rect(
        left = (1f - visibleWidth / displayWidth) / 2f,
        top = (1f - visibleHeight / displayHeight) / 2f,
        right = (1f + visibleWidth / displayWidth) / 2f,
        bottom = (1f + visibleHeight / displayHeight) / 2f,
    )
    val buffer = displayToBufferFraction(display, rotationDegrees)

    return IntRect(
        left = (buffer.left * bufferSize.width).roundToInt(),
        top = (buffer.top * bufferSize.height).roundToInt(),
        right = (buffer.right * bufferSize.width).roundToInt(),
        bottom = (buffer.bottom * bufferSize.height).roundToInt(),
    )
}

/** 표시 방향 비율 영역을 버퍼 비율 영역으로 옮긴다. */
private fun displayToBufferFraction(
    display: Rect,
    rotationDegrees: Int,
): Rect = when (rotationDegrees.mod(360)) {
    90 -> Rect(display.top, 1f - display.right, display.bottom, 1f - display.left)
    180 -> Rect(1f - display.right, 1f - display.bottom, 1f - display.left, 1f - display.top)
    270 -> Rect(1f - display.bottom, display.left, 1f - display.top, display.right)
    else -> display
}

/** 크롭과 회전을 `createBitmap` 한 번으로 처리해 저장한다. `cropRect`가 null 이면 전체를 회전만 한다. */
internal fun saveCroppedCapture(
    captured: Bitmap,
    cropRect: IntRect?,
    rotationDegrees: Int,
    file: File,
) {
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    val region = cropRect ?: IntRect(0, 0, captured.width, captured.height)
    val result = Bitmap.createBitmap(captured, region.left, region.top, region.width, region.height, matrix, true)

    file.outputStream().use { output ->
        result.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
    }
}

private const val JPEG_QUALITY = 95
