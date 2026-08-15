package com.teamyg.parfait.core.util.jvm.extension

import kotlin.math.max

/** 아직 거리를 재지 않은 칸에 넣어 둘 값. 그 판에서 나올 수 있는 어떤 제곱거리보다 크기만 하면 된다 */
const val SQUARED_DISTANCE_UNSET = 1e10f

/**
 * 값이 0 인 칸을 씨앗으로 보고, 칸마다 가장 가까운 씨앗까지의 제곱거리를 재 제자리에 채운다.
 *
 * 씨앗이 아닌 칸은 [SQUARED_DISTANCE_UNSET] 으로 채워 넘기면 된다.
 * 씨앗이 하나도 없으면 모든 칸이 그 값 언저리에 남는다.
 *
 * 가로로 한 번 세로로 한 번 훑는 방식이라 판이 아무리 커도 칸 수에 비례한 시간만 든다.
 * 결과를 돌려주지 않고 받은 배열을 고치는 것은, 판만 한 배열을 한 벌 더 잡지 않기 위해서다.
 */
fun FloatArray.fillWithSquaredDistance(
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
        fillLine(line, height, transformed, nearestIndices, nearestBoundaries)
        for (y in 0 until height) this[y * width + x] = transformed[y]
    }

    // 세로줄에서 잰 거리를 그대로 물려받아 가로로 한 번 더 훑으면 두 방향을 함께 잰 거리가 남는다
    for (y in 0 until height) {
        val rowStart = y * width
        for (x in 0 until width) line[x] = this[rowStart + x]
        fillLine(line, width, transformed, nearestIndices, nearestBoundaries)
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
private fun fillLine(
    line: FloatArray,
    count: Int,
    transformed: FloatArray,
    nearestIndices: IntArray,
    nearestBoundaries: FloatArray,
) {
    var nearestCount = 0
    nearestIndices[0] = 0
    nearestBoundaries[0] = -SQUARED_DISTANCE_UNSET
    nearestBoundaries[1] = SQUARED_DISTANCE_UNSET

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
        nearestBoundaries[nearestCount + 1] = SQUARED_DISTANCE_UNSET
    }

    nearestCount = 0
    for (index in 0 until count) {
        while (nearestBoundaries[nearestCount + 1] < index) nearestCount++
        val nearest = nearestIndices[nearestCount]
        val gap = (index - nearest).toFloat()
        transformed[index] = gap * gap + line[nearest]
    }
}
