package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.compose.ui.geometry.Offset

/**
 * 편집 모드. 마스크를 넓히거나 좁히는 두 방향만 존재한다.
 */
enum class SegmentationEditMode {
    /** 원본에서 픽셀을 되살려 영역을 넓힌다 */
    ADD,

    /** 잘라낸 영역에서 픽셀을 걷어낸다 */
    ERASE,
}

/**
 * 사용자가 한 번 드래그해서 그린 획.
 *
 * [points] 와 [width] 는 화면이 아니라 **원본 비트맵 좌표계** 기준이다.
 * 화면 크기나 회전이 바뀌어도 편집 결과가 따라 변하지 않도록 하기 위함이다.
 */
data class SegmentationEditStroke(
    val mode: SegmentationEditMode,
    val points: List<Offset>,
    val width: Float,
)
