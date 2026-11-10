package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

private val GUIDE_LINE_WIDTH = 1.dp
private val GUIDE_DASH_ON_LENGTH = 2.4.dp
private val GUIDE_DASH_GAP_LENGTH = 4.8.dp

/**
 * @param centerOffset 토핑 중심의 캔버스 로컬 좌표 — 가로선이면 Y, 세로선이면 X 값만 쓴다.
 * @param isHorizontal true 면 0/180도 스냅(가로선), false 면 90/270도 스냅(세로선).
 */
@Composable
internal fun AngleGuideLines(
    isVisible: Boolean,
    isHorizontal: Boolean,
    centerOffset: Dp,
    modifier: Modifier = Modifier,
    color: Color = YGAtomicColors.Melon.Melon500,
    lineWidth: Dp = GUIDE_LINE_WIDTH,
    dashOnLength: Dp = GUIDE_DASH_ON_LENGTH,
    dashGapLength: Dp = GUIDE_DASH_GAP_LENGTH,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = guideLineFadeIn(),
        exit = guideLineFadeOut(),
        modifier = modifier,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = lineWidth.toPx()
            val pathEffect = PathEffect.dashPathEffect(
                intervals = floatArrayOf(dashOnLength.toPx(), dashGapLength.toPx()),
                phase = 0f,
            )
            if (isHorizontal) {
                val y = centerOffset.toPx()
                drawLine(
                    color = color,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Square,
                    pathEffect = pathEffect,
                )
            } else {
                val x = centerOffset.toPx()
                drawLine(
                    color = color,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Square,
                    pathEffect = pathEffect,
                )
            }
        }
    }
}
