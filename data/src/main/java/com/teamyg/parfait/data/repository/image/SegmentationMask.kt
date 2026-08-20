package com.teamyg.parfait.data.repository.image

import com.teamyg.parfait.domain.model.SegmentationBounds
import java.nio.FloatBuffer

/** 이 값을 **넘는** 신뢰도만 객체로 본다 */
internal const val SUBJECT_CONFIDENCE_THRESHOLD = 0.5f

private const val TRANSPARENT = 0

/**
 * [pixels] 에서 객체가 아닌 자리를 투명으로 지우고, 남은 자리를 감싸는 사각 영역을 돌려준다.
 *
 * 넘겨받은 배열을 **그 자리에서** 고친다. 결과용 배열을 따로 만들면 큰 사진에서 같은 크기 배열을
 * 둘 들고 있게 되는데, 이 함수가 도는 동안엔 원본 비트맵도 아직 살아 있다.
 *
 * `Bitmap` 을 받지 않는 것은 이 판단(임계·경계 계산)을 기기 없이 검증하기 위해서다.
 *
 * @param mask 픽셀별 전경 신뢰도. 길이가 `width * height` 여야 한다 — 호출부가 검사한다
 * @return 객체 픽셀이 하나도 없으면 `null`
 */
internal fun maskSubjectPixels(
    pixels: IntArray,
    mask: FloatBuffer,
    width: Int,
    height: Int,
): SegmentationBounds? {
    var left = Int.MAX_VALUE
    var top = Int.MAX_VALUE
    var right = -1
    var bottom = -1

    for (index in 0 until width * height) {
        if (mask[index] > SUBJECT_CONFIDENCE_THRESHOLD) {
            val x = index % width
            val y = index / width

            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        } else {
            pixels[index] = TRANSPARENT
        }
    }

    if (left > right || top > bottom) return null

    // right·bottom 은 마지막 픽셀을 포함하도록 exclusive 로 담는다
    return SegmentationBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
}
