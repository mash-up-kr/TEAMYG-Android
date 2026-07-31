package com.teamyg.parfait.core.designsystem.component.ygcanvas

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
sealed interface YGCanvasBackground {
    @Immutable
    data class Solid(val color: Color) : YGCanvasBackground

    @Immutable
    data class Image(val url: String) : YGCanvasBackground
}
