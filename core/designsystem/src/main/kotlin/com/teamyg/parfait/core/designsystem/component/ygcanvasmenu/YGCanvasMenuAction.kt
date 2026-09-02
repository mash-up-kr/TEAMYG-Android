package com.teamyg.parfait.core.designsystem.component.ygcanvasmenu

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable

@Immutable
data class YGCanvasMenuAction(
    val text: String,
    @DrawableRes val iconResource: Int?,
    val onClick: () -> Unit,
    val isEnabled: Boolean = true,
)

@Immutable
data class YGCanvasMenuItem(
    val text: String,
    val onClick: () -> Unit,
)
