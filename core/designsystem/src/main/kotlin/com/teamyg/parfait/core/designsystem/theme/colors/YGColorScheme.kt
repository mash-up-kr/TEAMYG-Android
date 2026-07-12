package com.teamyg.parfait.core.designsystem.theme.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGColorScheme(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val info: Color,
    val grayScale: YGColorGrayScale,
    val transparency: YGColorTransparency,
)
