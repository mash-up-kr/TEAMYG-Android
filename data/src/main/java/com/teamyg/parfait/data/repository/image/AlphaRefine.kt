package com.teamyg.parfait.data.repository.image

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
