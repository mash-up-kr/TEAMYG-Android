package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_DESIGN_CANVAS_WIDTH
import com.teamyg.parfait.feature.groups.canvas.impl.model.TEXT_TOPPING_FONT_SIZE_SP

/** 기기 폭과 무관하게 캔버스 대비 같은 크기로 보이도록 디자인 값에 캔버스 배율을 곱한다 */
internal fun textToppingFontSizeSp(
    designFontSize: Float,
    canvasWidth: Dp,
): Float = designFontSize * (canvasWidth / TEXT_TOPPING_DESIGN_CANVAS_WIDTH)

/**
 * 글자 래핑 폭(배경 여백 제외). 자동 크기가 글자와 래핑 폭을 같은 비율로 줄이므로
 * 최대 크기일 때 캔버스 너비에서 같은 비율로 내려온다 — 그래서 줄바꿈 위치가 저장값만으로 재현된다.
 */
internal fun textToppingWrapWidth(
    designFontSize: Float,
    canvasWidth: Dp,
): Dp = canvasWidth * (designFontSize / TEXT_TOPPING_FONT_SIZE_SP)
