package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGTextFieldDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        backgroundColor: Color = YGTheme.colorScheme.transparency.white75,
        disabledBackgroundColor: Color = backgroundColor,
        borderColor: Color = YGAtomicColors.Gray.Gray100,
        focusedBorderColor: Color = YGAtomicColors.Cherry.Cherry200,
        errorBorderColor: Color = YGTheme.colorScheme.danger,
        textColor: Color = YGAtomicColors.Gray.Gray900,
        disabledTextColor: Color = textColor,
        placeholderColor: Color = YGAtomicColors.Gray.Gray300,
        cursorColor: Color = YGAtomicColors.Gray.Gray900,
        counterColor: Color = YGAtomicColors.Gray.Gray400,
        errorCounterColor: Color = YGTheme.colorScheme.danger,
        clearIconTint: Color = YGAtomicColors.Gray.Gray300,
    ): YGTextFieldColors = YGTextFieldColors(
        backgroundColor = backgroundColor,
        disabledBackgroundColor = disabledBackgroundColor,
        borderColor = borderColor,
        focusedBorderColor = focusedBorderColor,
        errorBorderColor = errorBorderColor,
        textColor = textColor,
        disabledTextColor = disabledTextColor,
        placeholderColor = placeholderColor,
        cursorColor = cursorColor,
        counterColor = counterColor,
        errorCounterColor = errorCounterColor,
        clearIconTint = clearIconTint,
    )
}
