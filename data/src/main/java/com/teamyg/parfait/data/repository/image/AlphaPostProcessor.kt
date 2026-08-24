package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds

private const val OPAQUE = 255

/**
 * [keep] 이 거짓인 자리의 알파를 0으로 만든다. 참인 자리는 **원본 알파를 그대로 둔다.**
 *
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun applyKeepMask(
    alpha: ByteArray,
    width: Int,
    height: Int,
    keep: BooleanArray,
    maskWidth: Int,
    factor: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        val maskRowOffset = (y / factor) * maskWidth
        for (x in 0 until width) {
            val index = rowOffset + x
            if ((alpha[index].toInt() and 0xFF) == 0) continue
            if (keep[maskRowOffset + x / factor]) continue

            alpha[index] = 0
            changed = true
        }
    }

    return changed
}

internal data class AlphaMeasurement(
    val bounds: SegmentationBounds,
    val alphaSum: Long,
    /** 알파가 1~254 인 픽셀 수. 관측이 램프 띠 폭과 침식 유효성을 판정하는 데 쓴다 */
    val partialAlphaPixels: Int,
)

/**
 * 남은 알파를 감싸는 사각 영역과 커버리지를 잰다.
 *
 * 불투명 판정을 "알파 0 초과"로 두는 근거는
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「tight bounds 판정 기준 통일」에 있다.
 *
 * @return 남은 알파가 없으면 `null`
 */
internal fun measureAlpha(
    alpha: ByteArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): AlphaMeasurement? {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1
    var sum = 0L
    var partial = 0

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        for (x in 0 until width) {
            val value = alpha[rowOffset + x].toInt() and 0xFF
            if (value == 0) continue

            sum += value
            if (value < OPAQUE) partial++
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    if (right < 0) return null

    return AlphaMeasurement(
        // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
        bounds = SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1),
        alphaSum = sum,
        partialAlphaPixels = partial,
    )
}

private const val ABSENT = -1

/**
 * 경계를 한 겹 안으로 깎는다. `a' = min(a, 4-근방 알파의 최소)` 다.
 *
 * ⚠️ **판정은 침식 전 값으로 한다.** 제자리에서 래스터 순서로 돌리면 방금 낮아진 왼쪽 값을 다음
 * 픽셀이 읽어, 부분 알파 띠가 스캔 방향으로만 연쇄 침식된다. 반대 방향은 한 겹만 깎이므로 피사체가
 * 한쪽으로 밀린 것처럼 보인다. 그래서 직전 행의 침식 전 알파 한 줄과 좌측 픽셀의 침식 전 값을
 * 들고 돈다. 오른쪽·아래는 아직 안 고쳤으므로 현재 배열을 그대로 읽는다.
 *
 * **능선 보호**: 마주 보는 4-근방 쌍(좌·우 또는 상·하)이 둘 다 0이면 건너뛴다.
 * `specs/2026-08-24-segmentation-mask-postprocessing.md` 「후처리 커널」 참고
 *
 * 이미지 밖 이웃은 최소 계산에서도 능선 판정에서도 빠진다. 밖을 투명으로 치면 프레임에 걸친
 * 피사체의 테두리가 깎인다.
 *
 * @return 알파가 한 픽셀이라도 바뀌었으면 true
 */
internal fun erodeEdge(
    alpha: ByteArray,
    width: Int,
    height: Int,
    checkCancelled: () -> Unit = {},
): Boolean {
    if (width <= 0 || height <= 0) return false

    var previousRow = ByteArray(width)
    var currentRow = ByteArray(width)
    var changed = false

    for (y in 0 until height) {
        checkCancelled()
        val rowOffset = y * width
        alpha.copyInto(currentRow, 0, rowOffset, rowOffset + width)

        var leftBefore = ABSENT
        for (x in 0 until width) {
            val here = currentRow[x].toInt() and 0xFF
            val left = leftBefore
            val right = if (x < width - 1) currentRow[x + 1].toInt() and 0xFF else ABSENT
            val up = if (y > 0) previousRow[x].toInt() and 0xFF else ABSENT
            val down = if (y < height - 1) alpha[rowOffset + width + x].toInt() and 0xFF else ABSENT
            leftBefore = here

            if (here == 0) continue
            if (left == 0 && right == 0) continue
            if (up == 0 && down == 0) continue

            var lowest = here
            if (left in 0 until lowest) lowest = left
            if (right in 0 until lowest) lowest = right
            if (up in 0 until lowest) lowest = up
            if (down in 0 until lowest) lowest = down

            if (lowest != here) {
                alpha[rowOffset + x] = lowest.toByte()
                changed = true
            }
        }

        val spare = previousRow
        previousRow = currentRow
        currentRow = spare
    }

    return changed
}
