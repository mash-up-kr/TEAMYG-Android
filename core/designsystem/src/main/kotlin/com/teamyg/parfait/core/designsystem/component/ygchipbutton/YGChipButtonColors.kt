package com.teamyg.parfait.core.designsystem.component.ygchipbutton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGChipButtonColors(
    val defaultForegroundColor: Color,
    val pressedForegroundColor: Color,
    val defaultBackgroundColor: Color,
    val pressedBackgroundColor: Color,
    val defaultBorderColor: Color,
    val pressedBorderColor: Color,
) {
    fun foregroundColor(isPressed: Boolean) = if (isPressed) pressedForegroundColor else defaultForegroundColor

    fun backgroundColor(isPressed: Boolean) = if (isPressed) pressedBackgroundColor else defaultBackgroundColor

    fun borderColor(isPressed: Boolean) = if (isPressed) pressedBorderColor else defaultBorderColor
}
