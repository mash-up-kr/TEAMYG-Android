package com.teamyg.parfait.data.repository.image

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job

/**
 * [value] 를 [divisor] 로 나누고 올린다. [value] 는 0 이상, [divisor] 는 1 이상이어야 한다.
 *
 * 0 을 돌려주고 넘어가지 않는 이유: [divisor] 가 0 이면 `factor` 로 나누는 나머지 세 자리
 * (`downscaleMask`·`applyKeepMask`·`minComponentPixels`)에서 같은 예외가 더 안쪽에서 난다.
 */
internal fun ceilDiv(
    value: Int,
    divisor: Int,
): Int {
    require(divisor > 0) { "divisor must be >= 1 but was $divisor" }

    return (value + divisor - 1) / divisor
}

/**
 * 알파를 [threshold] 로 이진화하고 [factor] × [factor] 블록마다 OR 해서 축소 마스크를 만든다.
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「처리 해상도」 참고
 */
internal suspend fun downscaleMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    factor: Int,
    threshold: Int,
): BooleanArray {
    val job = currentCoroutineContext().job
    val maskWidth = ceilDiv(width, factor)
    val mask = BooleanArray(maskWidth * ceilDiv(height, factor))

    for (y in 0 until height) {
        job.ensureActive()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            if ((alpha[rowOffset + x].toInt() and 0xFF) > threshold) {
                mask[maskRowOffset + x / factor] = true
            }
        }
    }

    return mask
}

/**
 * 픽셀 수가 [minPixels] 미만인 8-연결 성분을 [mask] 에서 그 자리에 지운다.
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * @return 살아남은 성분이 하나라도 있으면 true
 */
internal suspend fun applyAreaOpening(
    mask: BooleanArray,
    width: Int,
    height: Int,
    minPixels: Int,
): Boolean {
    val runCount = countRuns(mask, width, height)
    if (runCount == 0) return false

    val runRow = IntArray(runCount)
    val runStart = IntArray(runCount)
    val runEnd = IntArray(runCount)
    // rowFirstRun[y] 는 행 y 의 첫 런 인덱스다. 마지막 칸이 전체 런 개수라 y+1 을 안전하게 읽는다
    val rowFirstRun = IntArray(height + 1)
    fillRuns(mask, width, height, runRow, runStart, runEnd, rowFirstRun)

    val parent = IntArray(runCount) { it }
    unionAdjacentRows(height, runStart, runEnd, rowFirstRun, parent)

    val componentPixels = IntArray(runCount)
    for (run in 0 until runCount) {
        componentPixels[findRoot(parent, run)] += runEnd[run] - runStart[run]
    }

    var survived = false
    for (run in 0 until runCount) {
        if (componentPixels[findRoot(parent, run)] >= minPixels) {
            survived = true
            continue
        }
        val rowOffset = runRow[run] * width
        for (x in runStart[run] until runEnd[run]) mask[rowOffset + x] = false
    }

    return survived
}

private fun countRuns(
    mask: BooleanArray,
    width: Int,
    height: Int,
): Int {
    var count = 0
    for (y in 0 until height) {
        val rowOffset = y * width
        var x = 0
        while (x < width) {
            if (!mask[rowOffset + x]) {
                x++
                continue
            }
            count++
            while (x < width && mask[rowOffset + x]) x++
        }
    }
    return count
}

private fun fillRuns(
    mask: BooleanArray,
    width: Int,
    height: Int,
    runRow: IntArray,
    runStart: IntArray,
    runEnd: IntArray,
    rowFirstRun: IntArray,
) {
    var run = 0
    for (y in 0 until height) {
        rowFirstRun[y] = run
        val rowOffset = y * width
        var x = 0
        while (x < width) {
            if (!mask[rowOffset + x]) {
                x++
                continue
            }
            val start = x
            while (x < width && mask[rowOffset + x]) x++
            runRow[run] = y
            runStart[run] = start
            runEnd[run] = x
            run++
        }
    }
    rowFirstRun[height] = run
}

/**
 * 인접한 두 행의 런을 투 포인터로 훑어 잇는다.
 *
 * ⚠️ **한 런은 윗행의 겹치는 런 전부와 이어야 한다.** 첫 매치에서 멈추면 윗행 두 런을 아랫행 한
 * 런이 잇는 배치에서 성분이 갈린다. 그래서 조건이 맞아도 포인터를 멈추지 않고, 끝이 작은 쪽만
 * 전진시킨다.
 *
 * `xEnd` 가 exclusive 라 8-근방 겹침은 `aStart <= bEnd && bStart <= aEnd` 다.
 */
private suspend fun unionAdjacentRows(
    height: Int,
    runStart: IntArray,
    runEnd: IntArray,
    rowFirstRun: IntArray,
    parent: IntArray,
) {
    val job = currentCoroutineContext().job
    for (y in 0 until height - 1) {
        job.ensureActive()
        var upper = rowFirstRun[y]
        var lower = rowFirstRun[y + 1]
        val upperEnd = rowFirstRun[y + 1]
        val lowerEnd = rowFirstRun[y + 2]

        while (upper < upperEnd && lower < lowerEnd) {
            val touching = runStart[upper] <= runEnd[lower] && runStart[lower] <= runEnd[upper]
            if (touching) union(parent, upper, lower)

            if (runEnd[upper] < runEnd[lower]) upper++ else lower++
        }
    }
}

private fun findRoot(
    parent: IntArray,
    node: Int,
): Int {
    var root = node
    while (parent[root] != root) root = parent[root]

    var cursor = node
    while (parent[cursor] != root) {
        val next = parent[cursor]
        parent[cursor] = root
        cursor = next
    }

    return root
}

private fun union(
    parent: IntArray,
    left: Int,
    right: Int,
) {
    val leftRoot = findRoot(parent, left)
    val rightRoot = findRoot(parent, right)
    if (leftRoot != rightRoot) parent[rightRoot] = leftRoot
}

/**
 * 8-근방으로 1픽셀 팽창한 새 마스크를 돌려준다.
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「처리 해상도」 참고
 *
 * 반경 1이 계약이다 — 8-연결 성분끼리는 최소 거리가 2라, 반경을 키우면 area opening 이 지운
 * 성분이 되살아난다.
 */
internal suspend fun dilateMask(
    mask: BooleanArray,
    width: Int,
    height: Int,
): BooleanArray {
    val job = currentCoroutineContext().job
    val dilated = BooleanArray(mask.size)

    for (y in 0 until height) {
        job.ensureActive()
        for (x in 0 until width) {
            if (!mask[y * width + x]) continue

            for (neighborY in maxOf(0, y - 1)..minOf(height - 1, y + 1)) {
                for (neighborX in maxOf(0, x - 1)..minOf(width - 1, x + 1)) {
                    dilated[neighborY * width + neighborX] = true
                }
            }
        }
    }

    return dilated
}
