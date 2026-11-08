package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

private val GUIDE_LINE_WIDTH = 1.dp

/**
 * 토핑을 캔버스 정중앙으로 끌어왔을 때 뜨는 위치 가이드선(옐로우 실선)
 */
@Composable
internal fun CenterGuideLines(
    showVertical: Boolean,
    showHorizontal: Boolean,
    modifier: Modifier = Modifier,
    color: Color = YGAtomicColors.Pudding.Pudding500,
    lineWidth: Dp = GUIDE_LINE_WIDTH,
) {
    Box(modifier = modifier) {
        AnimatedVisibility(
            visible = showVertical,
            enter = guideLineFadeIn(),
            exit = guideLineFadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(lineWidth),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(color))
        }

        AnimatedVisibility(
            visible = showHorizontal,
            enter = guideLineFadeIn(),
            exit = guideLineFadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(lineWidth),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(color))
        }
    }
}
