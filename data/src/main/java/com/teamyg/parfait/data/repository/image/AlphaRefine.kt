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
