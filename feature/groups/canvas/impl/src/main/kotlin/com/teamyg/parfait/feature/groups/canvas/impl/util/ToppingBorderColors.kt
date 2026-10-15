package com.teamyg.parfait.feature.groups.canvas.impl.util

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * 테두리 패널에서 고를 수 있는 색. 나열 순서가 곧 화면에 깔리는 순서다.
 *
 * 맨 앞은 테두리를 두르지 않는 투명 칩이다.
 */
internal val TOPPING_BORDER_COLORS: List<Color> = listOf(
    YGAtomicColors.Gray.Transparent,
    YGAtomicColors.Gray.White,
    YGAtomicColors.Gray.Black,
    YGAtomicColors.Cherry.Cherry200,
    Color(0xFFFCE7C2),
    Color(0xFFF9F9AB),
    Color(0xFFC5FFD7),
    Color(0xFFC2E4FC),
    Color(0xFFDCC2FC),
)
