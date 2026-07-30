package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGButtonColors(
    val enabledForegroundColor: Color,
    val disabledForegroundColor: Color,
    val pressedForegroundColor: Color,
    val enabledBackgroundColor: Color,
    val disabledBackgroundColor: Color,
    val pressedBackgroundColor: Color,
    val enabledBorderColor: Color = Color.Transparent,
    val disabledBorderColor: Color = Color.Transparent,
    val pressedBorderColor: Color = Color.Transparent,
) {
    fun foregroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledForegroundColor
        isPressed -> pressedForegroundColor
        else -> enabledForegroundColor
    }

    fun backgroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBackgroundColor
        isPressed -> pressedBackgroundColor
        else -> enabledBackgroundColor
    }

    fun borderColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBorderColor
        isPressed -> pressedBorderColor
        else -> enabledBorderColor
    }
}
