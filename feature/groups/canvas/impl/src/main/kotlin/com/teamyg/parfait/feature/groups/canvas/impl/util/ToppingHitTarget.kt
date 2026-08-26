package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.TOPPING_OUTLINE_STAMP_COUNT
import kotlin.math.cos
import kotlin.math.sin

private const val FULL_TURN_DEGREES = 360.0

/**
 * 한 토핑의 판정 대상. 좌표는 모두 레이어 기준 픽셀이다.
 *
 * @param borderWidthPx 테두리 색이 실제로 정해졌을 때만 0 보다 크다. 그리지 않은 테두리만큼
 *   판정이 넓어지면 안 된다.
 * @param mask 아직 없거나 불투명 픽셀이 하나도 없으면 사각형 판정으로 떨어진다.
 */
data class ToppingHitTarget(
    val centerXPx: Float,
    val centerYPx: Float,
    val imageWidthPx: Float,
    val imageHeightPx: Float,
    val rotationDegrees: Float,
    val borderWidthPx: Float,
    val mask: ToppingAlphaMask?,
)

fun ToppingHitTarget.containsPoint(
    xPx: Float,
    yPx: Float,
): Boolean {
    val radians = Math.toRadians(-rotationDegrees.toDouble())
    val cosT = cos(radians).toFloat()
    val sinT = sin(radians).toFloat()

    val dx = xPx - centerXPx
    val dy = yPx - centerYPx
    val localX = dx * cosT - dy * sinT
    val localY = dx * sinT + dy * cosT

    val halfWidth = imageWidthPx / 2f + borderWidthPx
    val halfHeight = imageHeightPx / 2f + borderWidthPx
    if (localX < -halfWidth || localX > halfWidth) return false
    if (localY < -halfHeight || localY > halfHeight) return false

    // 마스크가 없거나 비어 있으면 사각형 판정이다 — 여기까지 왔으면 사각형 안이다
    if (mask?.hasAnyOpaque != true) return true

    if (isOpaqueAtLocal(localX, localY)) return true
    if (borderWidthPx <= 0f) return false

    // 테두리는 원본을 여덟 방향으로 밀어 찍은 것이라, 같은 방향으로 되민 점의 원본 알파를
    // 본다
    return (0 until TOPPING_OUTLINE_STAMP_COUNT).any { index ->
        val stampRadians = Math.toRadians(
            FULL_TURN_DEGREES / TOPPING_OUTLINE_STAMP_COUNT * index,
        )
        val offsetX = (cos(stampRadians) * borderWidthPx).toFloat()
        val offsetY = (sin(stampRadians) * borderWidthPx).toFloat()
        isOpaqueAtLocal(localX - offsetX, localY - offsetY)
    }
}

/** 그림 사각형 안의 좌표를 마스크 격자로 옮겨 읽는다. */
private fun ToppingHitTarget.isOpaqueAtLocal(
    localX: Float,
    localY: Float,
): Boolean {
    val usableMask = mask ?: return false
    val maskX =
        ((localX + imageWidthPx / 2f) * usableMask.width / imageWidthPx).toInt()
    val maskY =
        ((localY + imageHeightPx / 2f) * usableMask.height / imageHeightPx).toInt()
    return usableMask.isOpaqueAt(maskX, maskY)
}
