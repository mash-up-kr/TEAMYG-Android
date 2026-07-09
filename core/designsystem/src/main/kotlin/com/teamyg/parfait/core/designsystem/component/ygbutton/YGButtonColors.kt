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
    val enabledBorderColor: Color,
    val disabledBorderColor: Color,
    val pressedBorderColor: Color,
    // Todo
    //  icon color 랑 foregroundColor 가 따라가야할 것 같은데 분리되어있어서 일단 분리하였음
    //  이 또한 문의 예정
    val enabledIconColor: Color,
    val disabledIconColor: Color,
    val pressedIconColor: Color,
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

    fun iconColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledIconColor
        isPressed -> pressedIconColor
        else -> enabledIconColor
    }
}
