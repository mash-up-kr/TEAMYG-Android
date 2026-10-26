package com.teamyg.parfait.feature.groups.canvas.impl.model

import androidx.compose.ui.text.style.TextAlign

/** 기본값은 가운데 정렬. `[정렬]` 탭마다 가운데→왼쪽→오른쪽 순으로 순환한다 */
internal val TEXT_TOPPING_DEFAULT_ALIGN = TextAlign.Center

internal fun TextAlign.nextTextToppingAlign(): TextAlign = when (this) {
    TextAlign.Center -> TextAlign.Left
    TextAlign.Left -> TextAlign.Right
    else -> TextAlign.Center
}
