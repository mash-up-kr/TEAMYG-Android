package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.YGTheme

object YGTextFormFieldDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        textFieldColors: YGTextFieldColors = YGTextFieldDefaults.colors(),
        errorDescriptionColor: Color = YGTheme.colorScheme.danger,
    ): YGTextFormFieldColors = YGTextFormFieldColors(
        textFieldColors = textFieldColors,
        errorDescriptionColor = errorDescriptionColor,
    )
}
