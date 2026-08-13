package com.teamyg.parfait.feature.segmentation.impl.editor

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.teamyg.parfait.feature.segmentation.api.ToppingBorderLayer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 실루엣 안으로 볼 알파 문턱. 이보다 옅은 자리는 실루엣 바깥으로 친다 */
private const val OUTLINE_ALPHA_THRESHOLD = 128

/**
 * 거리를 잴 때 긴 변을 이 길이까지만 쓴다.
 *
 * 테두리 경계는 굵기만큼 완만한 곡선이라 촘촘히 재도 모양이 달라지지 않는데,
 * 원본 해상도로 재면 사진 크기에 비례해 시간과 메모리만 늘어난다.
 */
private const val DISTANCE_FIELD_MAX_DIMENSION = 1440

/** 아직 거리를 재지 않은 자리. 이 판에서 나올 수 있는 어떤 제곱거리보다 크기만 하면 된다 */
private const val SQUARED_DISTANCE_INFINITY = 1e10f

/** 가장자리 한 겹을 반 픽셀씩 물려 칠해 계단이 지지 않게 한다 */
private const val EDGE_FEATHER_PX = 0.5f

/**
 * 테두리 한 겹이 차지하는 구간.
 *
 * @param outsetPx 실루엣에서 이 겹의 바깥 끝까지 거리
 */
internal data class ToppingBorderBand(
    val outsetPx: Float,
    val colorArgb: Int,
)

/**
 * 겹을 구간으로 펴 놓는다.
 *
 * 겹은 아래 겹을 감싸며 쌓이므로 바깥 끝은 자기 굵기가 아니라 자기까지의 굵기를 모두 더한 값이다.
 *
 * 굵기는 dp 로 들고 있으므로 [pxPerDp] 를 곱해 그리는 쪽 좌표계로 환산한다.
 * 미리보기는 화면 px 로, 저장은 원본 비트맵 px 로 환산하는 값을 각각 넘긴다.
 */
internal fun List<ToppingBorderLayer>.toBorderBands(pxPerDp: Float): List<ToppingBorderBand> {
    var outsetDp = 0f
    return map { layer ->
        outsetDp += layer.widthDp
        ToppingBorderBand(outsetPx = outsetDp * pxPerDp, colorArgb = layer.colorArgb)
    }
}

/**
 * 실루엣에서 떨어진 거리를 픽셀마다 담아 둔 판.
 *
 * 실루엣 사본을 원 둘레에 빙 둘러 찍어 테두리를 만들면 굵어질수록 찍은 자국 사이가 벌어져
 * 가장자리가 삐죽삐죽해진다. 대신 거리를 한 번 재 두면 굵기는 '거리가 얼마 이하인 자리를 칠하는' 문제가 되어
 * 어떤 굵기에서도 가장자리가 실루엣에서 같은 거리인 곡선으로 이어진다.
 *
 * 굵기를 바꿔도 거리는 그대로라 슬라이더를 움직이는 동안에는 칠하는 일만 다시 하면 된다.
 */
internal class ToppingOutlineDistanceField internal constructor(
    private val width: Int,
    private val height: Int,
    /** 실루엣에서 떨어진 거리. 이 판의 좌표계 길이다 */
    private val distances: FloatArray,
) {
    /**
     * [bands] 를 안쪽부터 겹겹이 칠한 [targetWidth] x [targetHeight] 크기의 테두리 그림을 만든다.
     *
     * 알맹이는 이 위에 원래 자리 그대로 얹히므로 실루엣 안쪽도 가장 안쪽 겹 색으로 채워 둔다.
     * 알맹이가 있던 자리보다 넓어지지 않으니 바깥으로 나간 테두리는 자연히 잘린다.
     */
    fun buildBorderBitmap(
        targetWidth: Int,
        targetHeight: Int,
        bands: List<ToppingBorderBand>,
    ): Bitmap? {
        if (bands.isEmpty() || targetWidth <= 0 || targetHeight <= 0) return null

        // 거리는 이 판의 좌표계 길이라, 겹의 끝과 가장자리 물림도 같은 좌표계로 바꿔 재야 한다
        val fieldPerTargetPx = width.toFloat() / targetWidth
        val targetPxPerField = 1f / fieldPerTargetPx
        val edges = FloatArray(bands.size) { index -> bands[index].outsetPx * fieldPerTargetPx }
        val colors = IntArray(bands.size) { index -> bands[index].colorArgb }
        val outermostEdge = edges.last() + EDGE_FEATHER_PX * fieldPerTargetPx

        // 미리보기는 화면에 나올 크기 그대로 재 두므로 칸이 일대일로 맞는다. 그때는 섞지 않고 그 칸을 읽는다
        val fitsField = width == targetWidth && height == targetHeight

        val pixels = IntArray(targetWidth * targetHeight)
        for (y in 0 until targetHeight) {
            val fieldY = (y + 0.5f) * fieldPerTargetPx - 0.5f
            val rowStart = y * targetWidth

            for (x in 0 until targetWidth) {
                val distance = if (fitsField) {
                    distances[rowStart + x]
                } else {
                    distanceAt((x + 0.5f) * fieldPerTargetPx - 0.5f, fieldY)
                }
                if (distance > outermostEdge) continue

                var index = 0
                while (index < edges.lastIndex && distance > edges[index]) index++

                // 겹의 끝에 걸친 자리는 반씩 물려, 가장 바깥이면 투명하게 안쪽이면 다음 겹 색으로 이어 준다
                val coverage = ((edges[index] - distance) * targetPxPerField + EDGE_FEATHER_PX).coerceIn(0f, 1f)

                pixels[rowStart + x] = if (index == edges.lastIndex) {
                    fadeArgb(colors[index], coverage)
                } else {
                    mixArgb(colors[index], colors[index + 1], coverage)
                }
            }
        }

        return createBitmap(targetWidth, targetHeight)
            .apply { setPixels(pixels, 0, targetWidth, 0, 0, targetWidth, targetHeight) }
    }

    /** 판보다 넓은 그림을 칠할 수도 있어, 네 칸을 섞어 칸 사이 거리도 이어지게 읽는다 */
    private fun distanceAt(
        x: Float,
        y: Float,
    ): Float {
        val leftIndex = floor(x).toInt().coerceIn(0, width - 1)
        val topIndex = floor(y).toInt().coerceIn(0, height - 1)
        val rightIndex = (leftIndex + 1).coerceAtMost(width - 1)
        val bottomIndex = (topIndex + 1).coerceAtMost(height - 1)

        val rightWeight = (x - leftIndex).coerceIn(0f, 1f)
        val bottomWeight = (y - topIndex).coerceIn(0f, 1f)

        val top = distances.lerpAt(topIndex * width + leftIndex, topIndex * width + rightIndex, rightWeight)
        val bottom = distances.lerpAt(bottomIndex * width + leftIndex, bottomIndex * width + rightIndex, rightWeight)

        return top + (bottom - top) * bottomWeight
    }
}

private fun FloatArray.lerpAt(
    fromIndex: Int,
    toIndex: Int,
    weight: Float,
): Float {
    val from = this[fromIndex]
    return from + (this[toIndex] - from) * weight
}

/** 색은 두고 알파만 [coverage] 만큼 남긴다 */
private fun fadeArgb(
    colorArgb: Int,
    coverage: Float,
): Int {
    val alpha = ((colorArgb ushr 24 and 0xFF) * coverage).roundToInt().coerceIn(0, 255)
    return (alpha shl 24) or (colorArgb and 0x00FFFFFF)
}

private val ARGB_CHANNEL_SHIFTS = intArrayOf(24, 16, 8, 0)

/** [coverage] 가 1 이면 [colorArgb], 0 이면 [otherArgb] 다 */
private fun mixArgb(
    colorArgb: Int,
    otherArgb: Int,
    coverage: Float,
): Int {
    var mixed = 0
    for (shift in ARGB_CHANNEL_SHIFTS) {
        val channel = (colorArgb ushr shift and 0xFF) * coverage + (otherArgb ushr shift and 0xFF) * (1f - coverage)
        mixed = mixed or (channel.roundToInt().coerceIn(0, 255) shl shift)
    }
    return mixed
}

/**
 * 알파가 남아 있는 자리를 실루엣으로 보고, 픽셀마다 실루엣에서 떨어진 거리를 잰다.
 *
 * 가로로 한 번 세로로 한 번 훑는 방식이라 굵기가 아무리 굵어도 그림 크기에 비례한 시간만 든다.
 */
internal fun Bitmap.toOutlineDistanceField(): ToppingOutlineDistanceField {
    val fieldScale = min(1f, DISTANCE_FIELD_MAX_DIMENSION.toFloat() / max(width, height))
    val fieldWidth = max(1, (width * fieldScale).roundToInt())
    val fieldHeight = max(1, (height * fieldScale).roundToInt())

    val source = if (fieldWidth == width && fieldHeight == height) this else scale(fieldWidth, fieldHeight)
    val pixels = IntArray(fieldWidth * fieldHeight)
    source.getPixels(pixels, 0, fieldWidth, 0, 0, fieldWidth, fieldHeight)
    if (source !== this) source.recycle()

    val squared = FloatArray(pixels.size) { index ->
        if (pixels[index] ushr 24 >= OUTLINE_ALPHA_THRESHOLD) 0f else SQUARED_DISTANCE_INFINITY
    }
    squared.transformSquaredDistance(fieldWidth, fieldHeight)

    return ToppingOutlineDistanceField(
        width = fieldWidth,
        height = fieldHeight,
        distances = FloatArray(squared.size) { index -> sqrt(squared[index]) },
    )
}

/** 세로줄을 먼저 훑고 그 결과를 가로줄로 한 번 더 훑으면 두 방향을 함께 잰 제곱거리가 남는다 */
private fun FloatArray.transformSquaredDistance(
    width: Int,
    height: Int,
) {
    val longest = max(width, height)
    val line = FloatArray(longest)
    val transformed = FloatArray(longest)
    val nearestIndices = IntArray(longest)
    val nearestBoundaries = FloatArray(longest + 1)

    for (x in 0 until width) {
        for (y in 0 until height) line[y] = this[y * width + x]
        transformLine(line, height, transformed, nearestIndices, nearestBoundaries)
        for (y in 0 until height) this[y * width + x] = transformed[y]
    }

    for (y in 0 until height) {
        val rowStart = y * width
        for (x in 0 until width) line[x] = this[rowStart + x]
        transformLine(line, width, transformed, nearestIndices, nearestBoundaries)
        for (x in 0 until width) this[rowStart + x] = transformed[x]
    }
}

/**
 * 한 줄에서 각 자리까지의 최소 제곱거리를 구한다.
 *
 * 자리마다 그 자리를 꼭짓점으로 하는 포물선을 세우고 가장 아래에 깔리는 조각만 남기는 방식이라,
 * 자리마다 온 줄을 다시 보지 않고 줄 길이에 비례한 시간만 든다.
 *
 * [nearestIndices] 는 남은 조각의 꼭짓점, [nearestBoundaries] 는 조각이 바뀌는 자리다.
 * 줄마다 새로 만들지 않도록 밖에서 받아 돌려 쓴다.
 */
private fun transformLine(
    line: FloatArray,
    count: Int,
    transformed: FloatArray,
    nearestIndices: IntArray,
    nearestBoundaries: FloatArray,
) {
    var nearestCount = 0
    nearestIndices[0] = 0
    nearestBoundaries[0] = -SQUARED_DISTANCE_INFINITY
    nearestBoundaries[1] = SQUARED_DISTANCE_INFINITY

    for (index in 1 until count) {
        var boundary: Float
        while (true) {
            val nearest = nearestIndices[nearestCount]
            val squaredGap = (index * index - nearest * nearest).toFloat()
            boundary = (line[index] - line[nearest] + squaredGap) / (2f * (index - nearest))
            if (boundary > nearestBoundaries[nearestCount] || nearestCount == 0) break
            nearestCount--
        }

        nearestCount++
        nearestIndices[nearestCount] = index
        nearestBoundaries[nearestCount] = boundary
        nearestBoundaries[nearestCount + 1] = SQUARED_DISTANCE_INFINITY
    }

    nearestCount = 0
    for (index in 0 until count) {
        while (nearestBoundaries[nearestCount + 1] < index) nearestCount++
        val nearest = nearestIndices[nearestCount]
        val gap = (index - nearest).toFloat()
        transformed[index] = gap * gap + line[nearest]
    }
}
