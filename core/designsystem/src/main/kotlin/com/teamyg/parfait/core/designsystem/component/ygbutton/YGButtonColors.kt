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
}
