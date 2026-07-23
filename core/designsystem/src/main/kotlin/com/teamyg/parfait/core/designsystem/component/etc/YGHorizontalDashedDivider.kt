package com.teamyg.parfait.core.designsystem.component.etc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGHorizontalDashedDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = SizeTokens.Size1.getDp(),
    dash: Dp = SizeTokens.Size4.getDp(),
    color: Color = YGAtomicColors.Gray.Gray100,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness),
    ) {
        val strokePx: Float = thickness.toPx()
        val dashPx: Float = dash.toPx()
        val centerY: Float = strokePx / 2

        drawLine(
            color = color,
            start = Offset(
                x = 0f,
                y = centerY,
            ),
            end = Offset(
                x = size.width,
                y = centerY,
            ),
            strokeWidth = strokePx,
            pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashPx, dashPx),
                phase = 0f,
            ),
        )
    }
}

@YGPreview
@Composable
private fun PreviewYGHorizontalDashedDivider() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        YGHorizontalDashedDivider()
    }
}
