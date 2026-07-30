package com.teamyg.parfait.core.designsystem.component.ygcirclebutton

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Immutable
sealed interface YGCircleButtonType {
    val backgroundColor: Color

    val pressedBackgroundColor: Color

    val borderColor: Color

    val iconTint: Color

    val iconSize: Dp

    val paintsOuterCircle: Boolean

    data object Default : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.White
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray100
        override val borderColor: Color = YGAtomicColors.Transparency.Black5
        override val iconTint: Color = YGAtomicColors.Gray.Gray900
        override val iconSize: Dp = SizeTokens.Size28.getDp()
        override val paintsOuterCircle: Boolean = true
    }

    data object Secondary : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.Gray900
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray950
        override val borderColor: Color = YGAtomicColors.Transparency.White25
        override val iconTint: Color = YGAtomicColors.Gray.White
        override val iconSize: Dp = SizeTokens.Size28.getDp()
        override val paintsOuterCircle: Boolean = true
    }

    data object Small : YGCircleButtonType {
        override val backgroundColor: Color = YGAtomicColors.Gray.White
        override val pressedBackgroundColor: Color = YGAtomicColors.Gray.Gray100
        override val borderColor: Color = YGAtomicColors.Transparency.Black5
        override val iconTint: Color = YGAtomicColors.Gray.Gray900
        override val iconSize: Dp = SizeTokens.Size18.getDp()
        override val paintsOuterCircle: Boolean = false
    }
}
