package com.teamyg.parfait.feature.camera.impl.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.min
import kotlin.math.roundToInt

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

/**
 * ViewPort 없이 바인딩됐을 때 FILL_CENTER 가 보여 주는 중앙 영역을 버퍼(회전 전) 좌표로 돌려준다.
 *
 * @return 피드나 버퍼 크기가 비면 null.
 */
internal fun fallbackCropRect(
    bufferSize: IntSize,
    rotationDegrees: Int,
    feedSize: Size,
): IntRect? {
    if (feedSize.width <= 0f || feedSize.height <= 0f) return null
    if (bufferSize.width <= 0 || bufferSize.height <= 0) return null

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
