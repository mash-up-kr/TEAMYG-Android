package com.teamyg.parfait.domain.model

/**
 * 감지된 객체를 감싸는 사각 영역. 원본 이미지의 픽셀 좌표 기준이다.
 *
 * [right] 와 [bottom] 은 마지막 픽셀을 포함하도록 exclusive 값으로 담는다.
 */
data class SegmentationBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val width: Int get() = right - left

    val height: Int get() = bottom - top
}
