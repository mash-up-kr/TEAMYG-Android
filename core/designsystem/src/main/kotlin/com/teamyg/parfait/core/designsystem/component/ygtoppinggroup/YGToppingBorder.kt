package com.teamyg.parfait.core.designsystem.component.ygtoppinggroup

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.util.jvm.outline.ToppingOutline

/**
 * @param outline `null` 은 거리판을 아직 못 받았다는 뜻이다 — 테두리 없음이 아니다
 */
@Immutable
data class YGToppingBorder(
    val color: Color,
    val width: Dp,
    val outline: ToppingOutline?,
)
