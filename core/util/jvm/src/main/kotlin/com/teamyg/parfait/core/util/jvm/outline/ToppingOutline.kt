package com.teamyg.parfait.core.util.jvm.outline

import com.teamyg.parfait.core.util.jvm.extension.SQUARED_DISTANCE_UNSET
import com.teamyg.parfait.core.util.jvm.extension.fadeArgb
import com.teamyg.parfait.core.util.jvm.extension.fillWithSquaredDistance
import com.teamyg.parfait.core.util.jvm.extension.mixArgb
import com.teamyg.parfait.core.util.jvm.model.ToppingBorderBand
import com.teamyg.parfait.core.util.jvm.model.ToppingBorderTarget
import com.teamyg.parfait.core.util.jvm.model.ToppingOutlineSpec
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 실루엣에서 떨어진 거리를 픽셀마다 담아 둔 판.
 *
 * 실루엣 사본을 원 둘레에 빙 둘러 찍어 테두리를 만들면 굵어질수록 찍은 자국 사이가 벌어져
 * 가장자리가 갈라진다. 거리를 한 번 재 두면 굵기는 '거리가 얼마 이하인 자리를 칠하는' 문제가 되어
 * 어떤 굵기에서도 가장자리가 실루엣에서 같은 거리인 곡선으로 이어진다.
 *
 * 굵기를 바꿔도 거리는 그대로라, 슬라이더를 움직이는 동안에는 칠하는 일만 다시 하면 된다.
 */
class ToppingOutline internal constructor(
    val width: Int,
    val height: Int,
    /** 실루엣까지의 거리를 [ToppingOutlineSpec.DISTANCE_STEPS_PER_PX] 눈금으로 담는다 */
    private val distances: ShortArray,
) {
    val hasAnySeed: Boolean = distances.any { step -> step.toInt() == 0 }

    /**
     * 판 좌표계 거리.
     *
     * 판 밖 좌표는 가장자리 값으로 고정하지 않고 벗어난 만큼을 함께 잰다. 씨앗이 모두 판 안에
     * 있으므로 `√(가장자리 거리² + 벗어난 거리²)` 가 참값의 하한이고, 가장 가까운 씨앗이 축에
     * 나란할 때 참값과 같다. 고정하면 실루엣이 판 변에 닿은 토핑에서 판 밖이 통째로 거리 0 이 된다.
     */
    fun distanceAt(
        x: Float,
        y: Float,
    ): Float {
        val clampedX = x.coerceIn(0f, (width - 1).toFloat())
        val clampedY = y.coerceIn(0f, (height - 1).toFloat())
        val inside = interpolatedAt(clampedX, clampedY)

        val overflowX = x - clampedX
        val overflowY = y - clampedY
        if (overflowX == 0f && overflowY == 0f) return inside

        return sqrt(inside * inside + overflowX * overflowX + overflowY * overflowY)
    }

    /**
     * [distanceAt] 과 달리 보간하지 않고 가장 가까운 칸을 읽는다 — 판정이 칸 단위로 답하던
     * 기존 동작을 그대로 유지하기 위해서다.
     */
    fun isOpaqueAt(
        x: Float,
        y: Float,
    ): Boolean {
        val cellX = floor(x + 0.5f).toInt()
        val cellY = floor(y + 0.5f).toInt()
        if (cellX < 0 || cellY < 0 || cellX >= width || cellY >= height) return false
        return distances[cellY * width + cellX].toInt() == 0
    }

    /**
     * 색을 태우지 않은 단색 띠. 칸마다 0~255 의 덮은 정도만 담는다.
     *
     * @return 다 훑기 전에 그만두었으면 `null` — 반쯤 칠한 판은 띠가 잘려 보인다
     */
    fun buildBorderAlpha(
        target: ToppingBorderTarget,
        outsetPx: Float,
        shouldContinue: () -> Boolean = { true },
    ): ByteArray? {
        if (!hasAnySeed || !target.isUsable || outsetPx <= 0f) return null

        val alpha = ByteArray(target.width * target.height)
        val completed = forEachBandPixel(target, floatArrayOf(outsetPx), shouldContinue) { index, _, coverage ->
            alpha[index] = (coverage * ToppingOutlineSpec.ALPHA_MAX).roundToInt().toByte()
        }
        return if (completed) alpha else null
    }

    /**
     * 겹을 안쪽부터 겹겹이 칠한 그림. 알맹이는 이 위에 원래 자리 그대로 얹히므로 실루엣 안쪽도
     * 가장 안쪽 겹 색으로 채워 둔다.
     *
     * @return 다 훑기 전에 그만두었으면 `null`
     */
    fun buildBorderPixels(
        target: ToppingBorderTarget,
        bands: List<ToppingBorderBand>,
        shouldContinue: () -> Boolean = { true },
    ): IntArray? {
        if (!hasAnySeed || bands.isEmpty() || bands.all { it.outsetPx <= 0f } || !target.isUsable) return null

        val colors = IntArray(bands.size) { index -> bands[index].colorArgb }
        val outsets = FloatArray(bands.size) { index -> bands[index].outsetPx }
        val pixels = IntArray(target.width * target.height)

        val completed = forEachBandPixel(target, outsets, shouldContinue) { index, bandIndex, coverage ->
            // 겹의 끝에 걸친 자리는 반씩 물려, 가장 바깥이면 투명하게 안쪽이면 다음 겹 색으로 이어 준다
            pixels[index] = if (bandIndex == colors.lastIndex) {
                colors[bandIndex].fadeArgb(coverage)
            } else {
                colors[bandIndex].mixArgb(colors[bandIndex + 1], coverage)
            }
        }
        return if (completed) pixels else null
    }

    /**
     * 띠 안에 드는 칸만 골라 [onPixel] 에 넘긴다. 단색과 여러 겹이 이 순회를 함께 쓴다.
     *
     * 목표 좌표를 판 좌표로 옮길 때 알맹이가 놓인 자리를 빼고 축마다 따로 배율을 잰다. 굵기는
     * 등방이라 한 축으로만 환산하는데, 알맹이가 실루엣 비율을 지켜 앉으므로 두 배율이 거의 같다.
     *
     * @param shouldContinue 행을 시작하기 전에만 묻는다 — 칸마다 물으면 묻는 값이 본 계산을 넘는다
     * @return 끝까지 훑었으면 `true`
     */
    private inline fun forEachBandPixel(
        target: ToppingBorderTarget,
        outsetsPx: FloatArray,
        shouldContinue: () -> Boolean,
        onPixel: (index: Int, bandIndex: Int, coverage: Float) -> Unit,
    ): Boolean {
        val fieldPerTargetX = width.toFloat() / target.subjectWidth
        val fieldPerTargetY = height.toFloat() / target.subjectHeight
        val targetPxPerField = 1f / fieldPerTargetX

        val edges = FloatArray(outsetsPx.size) { index -> outsetsPx[index] * fieldPerTargetX }
        val outermostEdge = edges.last() + ToppingOutlineSpec.EDGE_FEATHER_PX * fieldPerTargetX

        for (y in 0 until target.height) {
            if (!shouldContinue()) return false

            val fieldY = (y + 0.5f - target.subjectTop) * fieldPerTargetY - 0.5f
            val rowStart = y * target.width

            for (x in 0 until target.width) {
                val fieldX = (x + 0.5f - target.subjectLeft) * fieldPerTargetX - 0.5f
                val distance = distanceAt(fieldX, fieldY)
                if (distance > outermostEdge) continue

                var bandIndex = 0
                while (bandIndex < edges.lastIndex && distance > edges[bandIndex]) bandIndex++

                val coverage = ((edges[bandIndex] - distance) * targetPxPerField + ToppingOutlineSpec.EDGE_FEATHER_PX)
                    .coerceIn(0f, 1f)
                onPixel(rowStart + x, bandIndex, coverage)
            }
        }
        return true
    }

    /** 판 밖 보정을 태우지 않는 안쪽 전용 읽기. 네 칸을 섞어 칸 사이도 이어지게 한다 */
    private fun interpolatedAt(
        x: Float,
        y: Float,
    ): Float {
        val leftIndex = floor(x).toInt().coerceIn(0, width - 1)
        val topIndex = floor(y).toInt().coerceIn(0, height - 1)
        val rightIndex = (leftIndex + 1).coerceAtMost(width - 1)
        val bottomIndex = (topIndex + 1).coerceAtMost(height - 1)

        val rightWeight = (x - leftIndex).coerceIn(0f, 1f)
        val bottomWeight = (y - topIndex).coerceIn(0f, 1f)

        val topRow = topIndex * width
        val bottomRow = bottomIndex * width
        val top = lerp(rawAt(topRow + leftIndex), rawAt(topRow + rightIndex), rightWeight)
        val bottom = lerp(rawAt(bottomRow + leftIndex), rawAt(bottomRow + rightIndex), rightWeight)

        return lerp(top, bottom, bottomWeight)
    }

    private fun rawAt(index: Int): Float = distances[index].toInt().toFloat() / ToppingOutlineSpec.DISTANCE_STEPS_PER_PX

    private fun lerp(
        start: Float,
        stop: Float,
        fraction: Float,
    ): Float = start + (stop - start) * fraction

    companion object {
        /**
         * @param alphaAt 그 자리 픽셀의 알파를 0~255 로 답한다
         */
        fun of(
            width: Int,
            height: Int,
            alphaAt: (x: Int, y: Int) -> Int,
        ): ToppingOutline {
            val squared = FloatArray(width * height) { index ->
                val opaque = alphaAt(index % width, index / width) >= ToppingOutlineSpec.ALPHA_THRESHOLD
                if (opaque) 0f else SQUARED_DISTANCE_UNSET
            }
            squared.fillWithSquaredDistance(width, height)

            val maxDistance = ToppingOutlineSpec.MAX_STORED_DISTANCE_PX.toFloat()

            return ToppingOutline(
                width = width,
                height = height,
                distances = ShortArray(squared.size) { index ->
                    val distance = sqrt(squared[index]).coerceAtMost(maxDistance)
                    (distance * ToppingOutlineSpec.DISTANCE_STEPS_PER_PX).roundToInt().toShort()
                },
            )
        }
    }
}
