package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class YGTextFormFieldColors(
    val textFieldColors: YGTextFieldColors,
    val descriptionColor: Color,
    val errorDescriptionColor: Color,
) {
    fun descriptionColor(isError: Boolean): Color = if (isError) errorDescriptionColor else descriptionColor
}
