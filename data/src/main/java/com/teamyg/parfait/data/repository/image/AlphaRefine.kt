package com.teamyg.parfait.data.repository.image

import kotlin.math.roundToInt

private const val OPAQUE = 255f

/**
 * 반경 [radius] 창의 평균을 낸다.
 *
 * ⚠️ 적분 영상을 쓰지 않는다. 12MP 판에서 누적값이 `Float` 정밀도를 넘어 창 차분이 무너진다.
 * 슬라이딩 합 2패스는 누적 구간이 한 행·한 열이라 그 문제가 없고 계산량도 같다.
 *
 * 가장자리에서는 창이 잘리므로 **실제 포함된 픽셀 수로 나눈다.**
 */
internal fun boxMean(
    src: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    checkCancelled: () -> Unit = {},
): FloatArray {
    val horizontal = FloatArray(src.size)
    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        var sum = 0f
        for (x in 0..minOf(radius, width - 1)) sum += src[rowOffset + x]
        for (x in 0 until width) {
            horizontal[rowOffset + x] = sum
            val exiting = x - radius
            val entering = x + radius + 1
            if (exiting >= 0) sum -= src[rowOffset + exiting]
            if (entering < width) sum += src[rowOffset + entering]
        }
    }

    val mean = FloatArray(src.size)
    for (x in 0 until width) {
        checkCancelled()
        var sum = 0f
        for (y in 0..minOf(radius, height - 1)) sum += horizontal[y * width + x]
        val columns = minOf(width - 1, x + radius) - maxOf(0, x - radius) + 1
        for (y in 0 until height) {
            val rows = minOf(height - 1, y + radius) - maxOf(0, y - radius) + 1
            mean[y * width + x] = sum / (columns * rows)
            val exiting = y - radius
            val entering = y + radius + 1
            if (exiting >= 0) sum -= horizontal[exiting * width + x]
            if (entering < height) sum += horizontal[entering * width + x]
        }
    }

    return mean
}

/**
 * 안내자를 휘도로 바꾸며 [factor] 배율로 줄인다. 컬러 3채널을 쓰지 않는 이유는
 * `specs/2026-08-25-segmentation-alpha-refinement.md` 「범위 - 제외」 참고.
 */
internal fun downscaleLuminance(
    pixels: IntArray,
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): FloatArray = downscale(width, height, factor, checkCancelled) { index -> luminanceOf(pixels[index]) }

internal fun downscaleAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): FloatArray = downscale(width, height, factor, checkCancelled) { index ->
    (alpha[index].toInt() and 0xFF) / OPAQUE
}

/**
 * 가장자리 블록은 **존재하는 칸만** 평균한다 — 없는 칸을 0으로 치면 오른쪽·아래 가장자리의
 * 안내자가 어두워져 경계가 그쪽으로 끌린다.
 */
private inline fun downscale(
    width: Int,
    height: Int,
    factor: Int,
    checkCancelled: () -> Unit,
    value: (Int) -> Float,
): FloatArray {
    val subWidth = ceilDiv(width, factor)
    val sums = FloatArray(subWidth * ceilDiv(height, factor))
    val counts = IntArray(sums.size)

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val subRowOffset = (y / factor) * subWidth
        for (x in 0 until width) {
            val index = subRowOffset + x / factor
            sums[index] += value(rowOffset + x)
            counts[index]++
        }
    }

    for (index in sums.indices) sums[index] /= counts[index]

    return sums
}

private fun luminanceOf(pixel: Int): Float {
    val red = (pixel ushr 16) and 0xFF
    val green = (pixel ushr 8) and 0xFF
    val blue = pixel and 0xFF
    return (0.299f * red + 0.587f * green + 0.114f * blue) / OPAQUE
}

internal class GuidedCoefficients(
    val a: FloatArray,
    val b: FloatArray,
)

/**
 * 창마다 `q = a·I + b` 의 계수를 구하고 그 계수를 다시 창 평균한다.
 *
 * `a` 는 안내자와 입력의 공분산을 안내자의 분산으로 나눈 값이라 **안내자에 경계가 있는 자리에서만
 * 커진다.** [epsilon] 이 크면 `a` 가 눌려 평균 필터로 퇴화한다.
 *
 * 근거는 `specs/2026-08-25-segmentation-alpha-refinement.md` 「설계 - 정련 알고리즘」에 있다.
 */
internal fun guidedCoefficients(
    guidance: FloatArray,
    input: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    epsilon: Float,
    checkCancelled: () -> Unit = {},
): GuidedCoefficients {
    val meanGuidance = boxMean(guidance, width, height, radius, checkCancelled)
    val meanInput = boxMean(input, width, height, radius, checkCancelled)
    val meanSquare = boxMean(
        FloatArray(guidance.size) { index -> guidance[index] * guidance[index] },
        width,
        height,
        radius,
        checkCancelled,
    )
    val meanProduct = boxMean(
        FloatArray(guidance.size) { index -> guidance[index] * input[index] },
        width,
        height,
        radius,
        checkCancelled,
    )

    val a = FloatArray(guidance.size)
    val b = FloatArray(guidance.size)
    for (y in 0 until height) {
        checkCancelled()
        for (x in 0 until width) {
            val index = y * width + x
            // 부동소수 오차로 음수가 나올 수 있다. 음수 분산은 a 의 부호를 뒤집는다
            val variance = maxOf(0f, meanSquare[index] - meanGuidance[index] * meanGuidance[index])
            val covariance = meanProduct[index] - meanGuidance[index] * meanInput[index]
            a[index] = covariance / (variance + epsilon)
            b[index] = meanInput[index] - a[index] * meanGuidance[index]
        }
    }

    return GuidedCoefficients(
        a = boxMean(a, width, height, radius, checkCancelled),
        b = boxMean(b, width, height, radius, checkCancelled),
    )
}

/**
 * 축소판 계수를 이중선형으로 되올려 원본 알파에 적용한다.
 *
 * **경계 선명도는 이 단계에서 나온다** — 계수는 저주파라 축소해도 되지만 곱해지는 안내자는 원본
 * 해상도다. nearest 로 되올리면 계수 자체의 블록 경계가 알파에 찍힌다.
 *
 * @param guidance ARGB. 휘도를 픽셀마다 즉석 계산한다 — 원본 해상도 실수 배열을 만들지 않는다
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun applyCoefficients(
    alpha: ByteArray,
    guidance: IntArray,
    coefficients: GuidedCoefficients,
    width: Int,
    height: Int,
    subWidth: Int,
    subHeight: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val sourceY = ((y + 0.5f) / factor - 0.5f).coerceIn(0f, (subHeight - 1).toFloat())
        val topRow = sourceY.toInt()
        val bottomRow = minOf(topRow + 1, subHeight - 1)
        val weightY = sourceY - topRow
        val rowOffset = y * width

        for (x in 0 until width) {
            val sourceX = ((x + 0.5f) / factor - 0.5f).coerceIn(0f, (subWidth - 1).toFloat())
            val leftColumn = sourceX.toInt()
            val rightColumn = minOf(leftColumn + 1, subWidth - 1)
            val weightX = sourceX - leftColumn

            val slope = bilinear(
                values = coefficients.a,
                stride = subWidth,
                leftColumn = leftColumn,
                rightColumn = rightColumn,
                topRow = topRow,
                bottomRow = bottomRow,
                weightX = weightX,
                weightY = weightY,
            )
            val offset = bilinear(
                values = coefficients.b,
                stride = subWidth,
                leftColumn = leftColumn,
                rightColumn = rightColumn,
                topRow = topRow,
                bottomRow = bottomRow,
                weightX = weightX,
                weightY = weightY,
            )

            val index = rowOffset + x
            val refined = slope * luminanceOf(guidance[index]) + offset
            val value = (refined * OPAQUE).roundToInt().coerceIn(0, 255)
            if (value != (alpha[index].toInt() and 0xFF)) {
                alpha[index] = value.toByte()
                changed = true
            }
        }
    }

    return changed
}

/**
 * 원본 휘도를 안내자로 알파 경계를 정련한다. 알파를 **그 자리에서** 고친다.
 *
 * 계수는 [downscale] 배율 축소판에서 구하고 적용만 원본 해상도에서 한다. 근거와 전체 설계는
 * `specs/2026-08-25-segmentation-alpha-refinement.md` 참고.
 *
 * @param guidance [alpha] 와 같은 크기·같은 좌표계의 ARGB. **ML Kit 이 배경을 도려낸 판이 아니라
 *   원본 사진에서 읽은 것이어야 한다** — 도려낸 판을 주면 안내자 경계가 알파 경계와 겹쳐
 *   정련이 지금 경계를 그대로 재현한다
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun refineAlpha(
    alpha: ByteArray,
    guidance: IntArray,
    width: Int,
    height: Int,
    downscale: Int,
    radius: Int,
    epsilon: Float,
    checkCancelled: () -> Unit = {},
): Boolean {
    require(alpha.size == width * height) {
        "alpha length ${alpha.size} does not match ${width}x$height"
    }
    require(guidance.size == alpha.size) {
        "guidance length ${guidance.size} does not match alpha length ${alpha.size}"
    }
    require(downscale >= 1) { "downscale must be at least 1 but was $downscale" }
    require(radius >= 1) { "radius must be at least 1 but was $radius" }
    require(epsilon > 0f) { "epsilon must be positive but was $epsilon" }
    if (width <= 0 || height <= 0) return false

    val subWidth = ceilDiv(width, downscale)
    val subHeight = ceilDiv(height, downscale)

    val coefficients = guidedCoefficients(
        guidance = downscaleLuminance(guidance, width, height, downscale, checkCancelled),
        input = downscaleAlpha(alpha, width, height, downscale, checkCancelled),
        width = subWidth,
        height = subHeight,
        radius = radius,
        epsilon = epsilon,
        checkCancelled = checkCancelled,
    )

    return applyCoefficients(
        alpha = alpha,
        guidance = guidance,
        coefficients = coefficients,
        width = width,
        height = height,
        subWidth = subWidth,
        subHeight = subHeight,
        factor = downscale,
        checkCancelled = checkCancelled,
    )
}

private fun bilinear(
    values: FloatArray,
    stride: Int,
    leftColumn: Int,
    rightColumn: Int,
    topRow: Int,
    bottomRow: Int,
    weightX: Float,
    weightY: Float,
): Float {
    val topOffset = topRow * stride
    val bottomOffset = bottomRow * stride
    val top = values[topOffset + leftColumn] * (1f - weightX) +
        values[topOffset + rightColumn] * weightX
    val bottom = values[bottomOffset + leftColumn] * (1f - weightX) +
        values[bottomOffset + rightColumn] * weightX
    return top * (1f - weightY) + bottom * weightY
}
