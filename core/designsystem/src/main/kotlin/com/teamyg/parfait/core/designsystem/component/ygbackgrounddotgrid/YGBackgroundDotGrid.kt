package com.teamyg.parfait.core.designsystem.component.ygbackgrounddotgrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

fun Modifier.ygBackgroundDotGrid(
    dotColor: Color = YGAtomicColors.Gray.Gray100,
    dotDiameter: Dp = SizeTokens.Size2.getDp(),
    spacing: Dp = SizeTokens.Size20.getDp(),
): Modifier = drawBehind {
    require(spacing > 0.dp) { "spacing은 0보다 커야합니다." }

    val spacingPx = spacing.toPx()
    val radiusPx = dotDiameter.toPx() / 2

    var y = 0f
    while (y <= size.height) {
        var x = 0f
        while (x <= size.width) {
            drawCircle(
                color = dotColor,
                radius = radiusPx,
                center = Offset(x, y),
            )
            x += spacingPx
        }
        y += spacingPx
    }
}

@YGPreview
@Composable
private fun YGBackgroundDotGridPreview() = PreviewBox {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YGAtomicColors.Gray.White)
            .ygBackgroundDotGrid(),
    )
}
