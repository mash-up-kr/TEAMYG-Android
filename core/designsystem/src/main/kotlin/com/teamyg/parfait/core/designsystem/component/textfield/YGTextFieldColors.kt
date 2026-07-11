package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGTextFieldColors(
    val backgroundColor: Color,
    val disabledBackgroundColor: Color,
    val borderColor: Color,
    val focusedBorderColor: Color,
    val errorBorderColor: Color,
    val textColor: Color,
    val disabledTextColor: Color,
    val placeholderColor: Color,
    val cursorColor: Color,
    val counterColor: Color,
    val errorCounterColor: Color,
    val clearIconTint: Color,
) {
    fun backgroundColor(isEnabled: Boolean): Color = if (isEnabled) backgroundColor else disabledBackgroundColor

    fun borderColor(
        isEnabled: Boolean,
        isFocused: Boolean,
        isError: Boolean,
    ): Color = when {
        isEnabled.not() -> borderColor
        isError -> errorBorderColor
        isFocused -> focusedBorderColor
        else -> borderColor
    }

    fun textColor(isEnabled: Boolean): Color = if (isEnabled) textColor else disabledTextColor

    fun counterColor(isError: Boolean): Color = if (isError) errorCounterColor else counterColor
}
