package com.teamyg.parfait.core.designsystem.shape

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Figma 캔버스 영역
 */
fun canvasCutCornerShape(cutSize: Dp = DefaultCutSize): Shape = CanvasCutCornerShape(cutSize)

private val DefaultCutSize: Dp = 17.dp

@Immutable
private data class CanvasCutCornerShape(private val cutSize: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cut = with(density) { cutSize.toPx() }
            .coerceAtMost(minOf(size.width, size.height))
        val path = Path().apply {
            moveTo(cut, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            lineTo(0f, cut)
            close()
        }
        return Outline.Generic(path)
    }
}
