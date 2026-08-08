package com.teamyg.parfait.feature.segmentation.impl.editor

import androidx.compose.ui.graphics.Color
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

/**
 * 테두리 탭에서 고를 수 있는 색. 나열 순서가 곧 화면에 깔리는 순서다.
 *
 * 맨 앞은 색을 얹지 않는 투명 칩이다.
 */
// Todo : 파스텔 다섯 색이 디자인 시스템에 올라가면 토큰으로 교체
val TOPPING_BORDER_COLORS: List<Color> = listOf(
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

/** 아직 아무 겹도 두르지 않은 상태를 가리키는 색 */
val DEFAULT_TOPPING_BORDER_COLOR: Color = TOPPING_BORDER_COLORS.first()
