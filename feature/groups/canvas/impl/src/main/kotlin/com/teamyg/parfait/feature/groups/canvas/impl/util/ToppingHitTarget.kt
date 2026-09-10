package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline
import kotlin.math.cos
import kotlin.math.sin

/**
 * 한 토핑의 판정 대상. 좌표는 모두 레이어 기준 픽셀이다.
 *
 * @param borderWidthPx 테두리 색이 실제로 정해졌을 때만 0 보다 크다. 그리지 않은 테두리만큼
 *   판정이 넓어지면 안 된다.
 * @param outline 아직 없거나 실루엣이 하나도 없으면 사각형 판정으로 떨어진다.
 */
data class ToppingHitTarget(
    val centerXPx: Float,
    val centerYPx: Float,
    val imageWidthPx: Float,
    val imageHeightPx: Float,
    val rotationDegrees: Float,
    val borderWidthPx: Float,
    val outline: ToppingOutline?,
) {
    fun containsPoint(
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

        // 실루엣을 못 읽으면 사각형 판정이다 — 여기까지 왔으면 사각형 안이다
        val usableOutline = outline?.takeIf { it.hasAnySeed } ?: return true

        // 테두리는 실루엣에서 굵기만큼 떨어진 자리까지라, 판정도 같은 거리로 답한다
        val fieldX = (localX + imageWidthPx / 2f) * usableOutline.width / imageWidthPx - 0.5f
        val fieldY = (localY + imageHeightPx / 2f) * usableOutline.height / imageHeightPx - 0.5f

        if (borderWidthPx <= 0f) return usableOutline.isOpaqueAt(fieldX, fieldY)

        val fieldPerImagePx = usableOutline.width / imageWidthPx
        return usableOutline.distanceAt(fieldX, fieldY) <= borderWidthPx * fieldPerImagePx
    }
}

/**
 * 겹친 것들 중 [xPx]·[yPx] 를 처음 받는 대상. 위 토핑의 투명한 자리는 통과해 아래가 잡힌다.
 *
 * @param entries 겹침 순서가 **아래에서 위**인 목록. 그리는 순서 그대로 넘기면 된다
 */
fun <T> pickToppingHit(
    entries: List<Pair<T, ToppingHitTarget>>,
    xPx: Float,
    yPx: Float,
): T? = entries
    .asReversed()
    .firstOrNull { (_, target) -> target.containsPoint(xPx, yPx) }
    ?.first
