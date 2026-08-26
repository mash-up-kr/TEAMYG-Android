package com.teamyg.parfait.feature.groups.canvas.impl.util

import com.teamyg.parfait.core.designsystem.component.ygtoppingcutout.TOPPING_OUTLINE_STAMP_COUNT
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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

    /**
     * 그림 사각형 안의 좌표를 마스크 격자로 옮겨 읽는다.
     *
     * 칸 번호는 [floor]로 내린다 — [Float.toInt]는 0 쪽으로 버려서 그림 왼쪽·위쪽 밖의 음수
     * 좌표가 전부 0번 칸으로 뭉개지고, 마스크의 범위 검사가 무력해진다.
     */
    private fun isOpaqueAtLocal(
        localX: Float,
        localY: Float,
    ): Boolean {
        val usableMask = mask ?: return false
        val maskX =
            floor((localX + imageWidthPx / 2f) * usableMask.width / imageWidthPx).toInt()
        val maskY =
            floor((localY + imageHeightPx / 2f) * usableMask.height / imageHeightPx).toInt()
        return usableMask.isOpaqueAt(maskX, maskY)
    }

    companion object {
        private const val FULL_TURN_DEGREES = 360.0
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
