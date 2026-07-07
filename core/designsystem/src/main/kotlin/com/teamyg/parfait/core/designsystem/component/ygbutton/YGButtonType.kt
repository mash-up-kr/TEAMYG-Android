package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.token.GapTokens
import com.teamyg.parfait.core.designsystem.theme.token.PaddingTokens
import com.teamyg.parfait.core.designsystem.theme.token.ShapeTokens
import com.teamyg.parfait.core.designsystem.theme.token.SizeToken
import com.teamyg.parfait.core.designsystem.theme.token.SizeTokens
import com.teamyg.parfait.core.designsystem.theme.typography.YGTypography

sealed interface YGButtonType {
    val iconGapSize: SizeToken
    val radius: Shape
    val textStyle: TextStyle
    val iconSize: SizeToken

    val startPadding: SizeToken
    val topPadding: SizeToken
    val endPadding: SizeToken
    val bottomPadding: SizeToken

    val colors: YGButtonColors

    // font style
    // radius

    // Todo : 뭔가 Design Token 이 규칙이 조금 이상한 것 같아서 컴포넌트 완성시점에 문의 예정.
    //  일단 작업을 위해 mock 으로 두었습니다.
    object XSmall : YGButtonType {
        override val iconGapSize: SizeToken = GapTokens.Gap2
        override val radius: Shape = ShapeTokens.RadiusRound
        override val textStyle: TextStyle = YGTypography.Body.B02_R
        override val iconSize: SizeToken = SizeTokens.Size16

        override val startPadding: SizeToken = PaddingTokens.Padding3
        override val topPadding: SizeToken = PaddingTokens.Padding2
        override val endPadding: SizeToken = PaddingTokens.Padding5
        override val bottomPadding: SizeToken = PaddingTokens.Padding2

        override val colors: YGButtonColors = YGButtonColors(
            enabledForegroundColor = YGAtomicColors.Gray.Gray600,
            disabledForegroundColor = YGAtomicColors.Gray.Gray600,
            pressedForegroundColor = YGAtomicColors.Gray.Gray700,
            enabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            disabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            pressedBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            enabledBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            pressedBorderColor = YGAtomicColors.Cherry.Cherry100,
            enabledIconColor = YGAtomicColors.Gray.Black,
            disabledIconColor = YGAtomicColors.Gray.Black,
            pressedIconColor = YGAtomicColors.Gray.Black,
        )
    }

    // Todo : 뭔가 Design Token 이 규칙이 조금 이상한 것 같아서 컴포넌트 완성시점에 문의 예정.
    //  일단 작업을 위해 mock 으로 두었습니다.
    object Small : YGButtonType {
        override val iconGapSize: SizeToken = GapTokens.Gap2
        override val radius: Shape = ShapeTokens.RadiusXSmall
        override val textStyle: TextStyle = YGTypography.Body.B01_SB
        override val iconSize: SizeToken = SizeTokens.Size24

        override val startPadding: SizeToken = PaddingTokens.Padding5
        override val topPadding: SizeToken = PaddingTokens.Padding3
        override val endPadding: SizeToken = PaddingTokens.Padding5
        override val bottomPadding: SizeToken = PaddingTokens.Padding3

        override val colors: YGButtonColors = YGButtonColors(
            enabledForegroundColor = YGAtomicColors.Gray.Gray600,
            disabledForegroundColor = YGAtomicColors.Gray.Gray600,
            pressedForegroundColor = YGAtomicColors.Gray.Gray700,
            enabledBackgroundColor = YGAtomicColors.Gray.White,
            disabledBackgroundColor = YGAtomicColors.Gray.White,
            pressedBackgroundColor = YGAtomicColors.Gray.White,
            enabledBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            pressedBorderColor = Color.Transparent,
            enabledIconColor = YGAtomicColors.Gray.Black,
            disabledIconColor = YGAtomicColors.Gray.Black,
            pressedIconColor = YGAtomicColors.Gray.Black,
        )
    }

    object SmallSquare : YGButtonType {
        override val iconGapSize: SizeToken = GapTokens.Gap1
        override val radius: Shape = ShapeTokens.RadiusRound
        override val textStyle: TextStyle = YGTypography.Body.B02_SB
        override val iconSize: SizeToken = SizeTokens.Size24

        override val startPadding: SizeToken = PaddingTokens.Padding5
        override val topPadding: SizeToken = PaddingTokens.Padding4
        override val endPadding: SizeToken = PaddingTokens.Padding4
        override val bottomPadding: SizeToken = PaddingTokens.Padding3

        override val colors: YGButtonColors = YGButtonColors(
            enabledForegroundColor = YGAtomicColors.Gray.Gray600,
            disabledForegroundColor = YGAtomicColors.Gray.Gray600,
            pressedForegroundColor = YGAtomicColors.Gray.Gray700,
            enabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            disabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            pressedBackgroundColor = YGAtomicColors.Cherry.Cherry50,
            enabledBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            pressedBorderColor = Color.Transparent,
            enabledIconColor = YGAtomicColors.Gray.Black,
            disabledIconColor = YGAtomicColors.Gray.Black,
            pressedIconColor = YGAtomicColors.Gray.Black,
        )
    }

    object Medium {
        object Primary : YGButtonType {
            override val iconGapSize: SizeToken = GapTokens.Gap2
            override val radius: Shape = ShapeTokens.RadiusRound
            override val textStyle: TextStyle = YGTypography.Body.B01_SB
            override val iconSize: SizeToken = SizeTokens.Size24

            override val startPadding: SizeToken = PaddingTokens.Padding4
            override val topPadding: SizeToken = PaddingTokens.Padding5
            override val endPadding: SizeToken = PaddingTokens.Padding4
            override val bottomPadding: SizeToken = PaddingTokens.Padding5

            override val colors: YGButtonColors = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.White,
                disabledForegroundColor = YGAtomicColors.Gray.White,
                pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                disabledBackgroundColor = YGAtomicColors.Gray.Gray950,
                pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                enabledBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                pressedBorderColor = Color.Transparent,
                enabledIconColor = YGAtomicColors.Gray.White,
                disabledIconColor = YGAtomicColors.Gray.White,
                pressedIconColor = YGAtomicColors.Gray.Gray500,
            )
        }

        object Secondary : YGButtonType {
            override val iconGapSize: SizeToken = GapTokens.Gap2
            override val radius: Shape = ShapeTokens.RadiusRound
            override val textStyle: TextStyle = YGTypography.Body.B01_SB
            override val iconSize: SizeToken = SizeTokens.Size24

            override val startPadding: SizeToken = PaddingTokens.Padding4
            override val topPadding: SizeToken = PaddingTokens.Padding5
            override val endPadding: SizeToken = PaddingTokens.Padding4
            override val bottomPadding: SizeToken = PaddingTokens.Padding5

            override val colors: YGButtonColors = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.Gray900,
                disabledForegroundColor = YGAtomicColors.Gray.Gray900,
                pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                enabledBackgroundColor = YGAtomicColors.Gray.Gray100,
                disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                enabledBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                pressedBorderColor = Color.Transparent,
                enabledIconColor = YGAtomicColors.Gray.Gray900,
                disabledIconColor = YGAtomicColors.Gray.Gray900,
                pressedIconColor = YGAtomicColors.Gray.Gray500,
            )
        }

        object Transparency : YGButtonType {
            override val iconGapSize: SizeToken = GapTokens.Gap2
            override val radius: Shape = ShapeTokens.RadiusRound
            override val textStyle: TextStyle = YGTypography.Body.B01_SB
            override val iconSize: SizeToken = SizeTokens.Size24

            override val startPadding: SizeToken = PaddingTokens.Padding4
            override val topPadding: SizeToken = PaddingTokens.Padding5
            override val endPadding: SizeToken = PaddingTokens.Padding4
            override val bottomPadding: SizeToken = PaddingTokens.Padding5

            override val colors: YGButtonColors = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.Gray900,
                disabledForegroundColor = YGAtomicColors.Gray.Gray900,
                pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                enabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                disabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.9f),
                pressedBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                enabledBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                pressedBorderColor = Color.Transparent,
                enabledIconColor = YGAtomicColors.Gray.Gray900,
                disabledIconColor = YGAtomicColors.Gray.Gray900,
                pressedIconColor = YGAtomicColors.Gray.Gray500,
            )
        }
    }

    object Large : YGButtonType {
        override val iconGapSize: SizeToken = GapTokens.Gap2
        override val radius: Shape = ShapeTokens.RadiusRound
        override val textStyle: TextStyle = YGTypography.Body.B01_SB
        override val iconSize: SizeToken = SizeTokens.Size24

        override val startPadding: SizeToken = PaddingTokens.Padding4
        override val topPadding: SizeToken = PaddingTokens.Padding5
        override val endPadding: SizeToken = PaddingTokens.Padding4
        override val bottomPadding: SizeToken = PaddingTokens.Padding5

        override val colors: YGButtonColors = YGButtonColors(
            enabledForegroundColor = YGAtomicColors.Gray.White,
            disabledForegroundColor = YGAtomicColors.Gray.White,
            pressedForegroundColor = YGAtomicColors.Gray.Gray500,
            enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
            disabledBackgroundColor = YGAtomicColors.Gray.Gray950,
            pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
            enabledBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            pressedBorderColor = Color.Transparent,
            enabledIconColor = YGAtomicColors.Gray.White,
            disabledIconColor = YGAtomicColors.Gray.White,
            pressedIconColor = YGAtomicColors.Gray.Gray500,
        )
    }
}
