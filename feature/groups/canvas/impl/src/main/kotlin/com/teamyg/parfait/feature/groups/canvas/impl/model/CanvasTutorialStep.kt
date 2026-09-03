package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.teamyg.parfait.feature.groups.canvas.impl.R

/** 설명 카드가 화면 위·아래 중 어디에 붙는가. 카드가 그 장에서 강조하는 UI 를 덮지 않는 쪽이다 */
enum class TutorialBoxPlacement {
    Top,
    Bottom,
}

/**
 * 캔버스 튜토리얼 한 장. **선언 순서가 곧 노출 순서**라 바꾸면 화면이 바뀐다.
 *
 * 이미지는 알파 없는 풀스크린 목업이다 — 강조할 자리만 뚫린 오버레이가 아니라, 딤까지 구워진
 * 캔버스 화면 한 장이 통째로 실제 화면을 덮는다.
 */
enum class CanvasTutorialStep(
    @DrawableRes val imageResource: Int,
    @StringRes val titleResource: Int,
    @StringRes val descriptionResource: Int,
    val boxPlacement: TutorialBoxPlacement,
) {
    // 이 장이 강조하는 것은 상단 날짜 버튼과 그 아래로 펼쳐진 달력이다 — 카드를 위에 두면
    // 정작 보여 줘야 할 자리를 카드가 덮는다
    Calendar(
        imageResource = R.drawable.img_canvas_tutorial_1,
        titleResource = R.string.canvas_tutorial_calendar_title,
        descriptionResource = R.string.canvas_tutorial_calendar_description,
        boxPlacement = TutorialBoxPlacement.Bottom,
    ),
    ToppingAdd(
        imageResource = R.drawable.img_canvas_tutorial_2,
        titleResource = R.string.canvas_tutorial_topping_add_title,
        descriptionResource = R.string.canvas_tutorial_topping_add_description,
        boxPlacement = TutorialBoxPlacement.Top,
    ),
    CanvasEdit(
        imageResource = R.drawable.img_canvas_tutorial_3,
        titleResource = R.string.canvas_tutorial_canvas_edit_title,
        descriptionResource = R.string.canvas_tutorial_canvas_edit_description,
        boxPlacement = TutorialBoxPlacement.Top,
    ),
    ;

    /** 사람이 읽는 순번. 칩에 `2/3` 으로 찍힌다 */
    val stepNumber: Int
        get() = ordinal + 1

    /** 마지막 장이면 `null` — 그 자리에서 튜토리얼이 끝난다 */
    val next: CanvasTutorialStep?
        get() = entries.getOrNull(ordinal + 1)

    companion object {
        val first: CanvasTutorialStep
            get() = entries.first()

        val totalCount: Int
            get() = entries.size
    }
}
