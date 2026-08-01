package com.teamyg.parfait.feature.camera.impl.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import java.io.File
import kotlin.math.max
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
private const val JPEG_QUALITY = 95

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
