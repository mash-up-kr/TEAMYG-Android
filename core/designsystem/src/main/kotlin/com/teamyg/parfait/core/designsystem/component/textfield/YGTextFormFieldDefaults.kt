package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

object YGTextFormFieldDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        textFieldColors: YGTextFieldColors = YGTextFieldDefaults.colors(),
        descriptionColor: Color = YGAtomicColors.Gray.Gray400,
        errorDescriptionColor: Color = YGTheme.colorScheme.danger,
    ): YGTextFormFieldColors = YGTextFormFieldColors(
        textFieldColors = textFieldColors,
        descriptionColor = descriptionColor,
        errorDescriptionColor = errorDescriptionColor,
    )
}
