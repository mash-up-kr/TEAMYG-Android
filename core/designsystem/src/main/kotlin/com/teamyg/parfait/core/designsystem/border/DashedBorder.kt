package com.teamyg.parfait.core.designsystem.border

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

fun Modifier.dashedBorder(
    color: Color = YGAtomicColors.Gray.Gray100,
    stroke: Dp = SizeTokens.Size1.getDp(),
    dash: Dp = SizeTokens.Size4.getDp(),
): Modifier = this.drawBehind {
    val strokePx: Float = stroke.toPx()
    val dashPx: Float = dash.toPx()

    this.drawRoundRect(
        color = color,
        topLeft = Offset(
            x = strokePx / 2,
            y = strokePx / 2,
        ),
        size = Size(
            width = size.width - strokePx,
            height = size.height - strokePx,
        ),
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashPx, dashPx),
                phase = 0f,
            ),
        ),
    )
}
