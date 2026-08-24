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
