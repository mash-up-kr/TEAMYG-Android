package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.feature.segmentation.impl.R

/**
 * 편집 화면의 하단 탭. 탭마다 만지는 대상이 다르다.
 *
 * @param label 탭 버튼에 띄울 문구. 탭을 늘릴 때 문구를 빠뜨리지 않도록 여기에 함께 둔다
 */
enum class ToppingEditTab(
    @StringRes val label: Int,
) {
    /** 잘라낼 영역 자체를 지우거나 되살린다 */
    AREA(R.string.topping_edit_tab_area),

    /** 잘라낸 결과에 두를 테두리를 다듬는다 */
    BORDER(R.string.topping_edit_tab_border),
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

/**
 * 테두리 탭에서 한 번에 두른 테두리 한 겹.
 *
 * 색상칩을 고를 때마다 그 시점의 굵기로 한 겹이 위에 쌓여 중첩된다.
 * 되돌리기는 이 겹 단위로 가장 바깥부터 벗겨낸다.
 *
 * [width] 는 [ToppingEditStroke] 와 같이 **원본 비트맵 좌표계** 기준이다.
 * 화면에서 본 굵기와 저장된 결과의 굵기가 어긋나지 않으려면 화면 크기에 매이지 않아야 한다.
 */
data class ToppingBorderStroke(
    val color: Color,
    val width: Float,
)
