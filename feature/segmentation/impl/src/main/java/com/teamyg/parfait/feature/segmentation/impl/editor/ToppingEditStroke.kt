package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.compose.ui.geometry.Offset

/**
 * 편집 화면의 하단 탭. 탭마다 만지는 대상이 다르다.
 */
enum class ToppingEditTab {
    /** 잘라낼 영역 자체를 지우거나 되살린다 */
    AREA,

    /** 잘라낸 결과에 두를 테두리를 다듬는다 */
    BORDER,
}

/**
 * 영역 탭의 편집 모드. 마스크를 넓히거나 좁히는 두 방향만 존재한다.
 */
enum class ToppingEditMode {
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
data class ToppingEditStroke(
    val mode: ToppingEditMode,
    val points: List<Offset>,
    val width: Float,
)
