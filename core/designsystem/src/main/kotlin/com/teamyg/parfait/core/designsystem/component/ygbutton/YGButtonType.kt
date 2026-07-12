package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

sealed interface YGButtonType {
    @get:Composable
    val iconGapSize: Dp

    @get:Composable
    val radius: Shape

    @get:Composable
    val textStyle: TextStyle

    val iconSize: Dp

    @get:Composable
    val startPadding: Dp

    @get:Composable
    val topPadding: Dp

    @get:Composable
    val endPadding: Dp

    @get:Composable
    val bottomPadding: Dp

    @get:Composable
    val colors: YGButtonColors

    // font style
    // radius

    // Todo : 뭔가 Design Token 이 규칙이 조금 이상한 것 같아서 컴포넌트 완성시점에 문의 예정.
    //  일단 작업을 위해 mock 으로 두었습니다.
    object XSmall : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap2

        override val radius: Shape
            @Composable
            get() = YGTheme.shapes.radius.round

        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b02R

        override val iconSize: Dp
            get() = SizeTokens.Size16.getDp()

        override val startPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding3

        override val topPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding2

        override val endPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val bottomPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding2

        override val colors: YGButtonColors
            @Composable
            get() = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.Gray600,
                disabledForegroundColor = YGAtomicColors.Gray.Gray600,
                pressedForegroundColor = YGAtomicColors.Gray.Gray700,
                enabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                disabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                pressedBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                enabledBorderColor = YGAtomicColors.Gray.Transparent,
                disabledBorderColor = YGAtomicColors.Gray.Transparent,
                pressedBorderColor = YGAtomicColors.Cherry.Cherry100,
                enabledIconColor = YGAtomicColors.Gray.Black,
                disabledIconColor = YGAtomicColors.Gray.Black,
                pressedIconColor = YGAtomicColors.Gray.Black,
            )
    }

    // Todo : 뭔가 Design Token 이 규칙이 조금 이상한 것 같아서 컴포넌트 완성시점에 문의 예정.
    //  일단 작업을 위해 mock 으로 두었습니다.
    object Small : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap2

        override val radius: Shape
            @Composable
            get() = YGTheme.shapes.radius.xSmall

        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b01SB

        override val iconSize: Dp
            get() = SizeTokens.Size24.getDp()

        override val startPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val topPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding3

        override val endPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val bottomPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding3

        override val colors: YGButtonColors
            @Composable
            get() = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.Gray600,
                disabledForegroundColor = YGAtomicColors.Gray.Gray600,
                pressedForegroundColor = YGAtomicColors.Gray.Gray700,
                enabledBackgroundColor = YGAtomicColors.Gray.White,
                disabledBackgroundColor = YGAtomicColors.Gray.White,
                pressedBackgroundColor = YGAtomicColors.Gray.White,
                enabledBorderColor = YGAtomicColors.Gray.Transparent,
                disabledBorderColor = YGAtomicColors.Gray.Transparent,
                pressedBorderColor = YGAtomicColors.Gray.Transparent,
                enabledIconColor = YGAtomicColors.Gray.Black,
                disabledIconColor = YGAtomicColors.Gray.Black,
                pressedIconColor = YGAtomicColors.Gray.Black,
            )
    }

    object SmallSquare : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap1

        override val radius: Shape
            @Composable
            get() = YGTheme.shapes.radius.round

        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b02SB

        override val iconSize: Dp
            get() = SizeTokens.Size24.getDp()

        override val startPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val topPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding4

        override val endPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding4

        override val bottomPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding3

        override val colors: YGButtonColors
            @Composable
            get() = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.Gray600,
                disabledForegroundColor = YGAtomicColors.Gray.Gray600,
                pressedForegroundColor = YGAtomicColors.Gray.Gray700,
                enabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                disabledBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                pressedBackgroundColor = YGAtomicColors.Cherry.Cherry50,
                enabledBorderColor = YGAtomicColors.Gray.Transparent,
                disabledBorderColor = YGAtomicColors.Gray.Transparent,
                pressedBorderColor = YGAtomicColors.Gray.Transparent,
                enabledIconColor = YGAtomicColors.Gray.Black,
                disabledIconColor = YGAtomicColors.Gray.Black,
                pressedIconColor = YGAtomicColors.Gray.Black,
            )
    }

    object Medium {
        object Primary : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

            override val radius: Shape
                @Composable
                get() = YGTheme.shapes.radius.round

            override val textStyle: TextStyle
                @Composable
                get() = YGTheme.typography.body.b01SB

            override val iconSize: Dp
                get() = SizeTokens.Size24.getDp()

            override val startPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val topPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val endPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val bottomPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val colors: YGButtonColors
                @Composable
                get() = YGButtonColors(
                    enabledForegroundColor = YGAtomicColors.Gray.White,
                    disabledForegroundColor = YGAtomicColors.Gray.White,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                    enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                    disabledBackgroundColor = YGAtomicColors.Gray.Gray950,
                    pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                    enabledBorderColor = YGAtomicColors.Gray.Transparent,
                    disabledBorderColor = YGAtomicColors.Gray.Transparent,
                    pressedBorderColor = YGAtomicColors.Gray.Transparent,
                    enabledIconColor = YGAtomicColors.Gray.White,
                    disabledIconColor = YGAtomicColors.Gray.White,
                    pressedIconColor = YGAtomicColors.Gray.Gray500,
                )
        }

        object Secondary : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

            override val radius: Shape
                @Composable
                get() = YGTheme.shapes.radius.round

            override val textStyle: TextStyle
                @Composable
                get() = YGTheme.typography.body.b01SB

            override val iconSize: Dp
                get() = SizeTokens.Size24.getDp()

            override val startPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val topPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val endPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val bottomPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val colors: YGButtonColors
                @Composable
                get() = YGButtonColors(
                    enabledForegroundColor = YGAtomicColors.Gray.Gray900,
                    disabledForegroundColor = YGAtomicColors.Gray.Gray900,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                    enabledBackgroundColor = YGAtomicColors.Gray.Gray100,
                    disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                    pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                    enabledBorderColor = YGAtomicColors.Gray.Transparent,
                    disabledBorderColor = YGAtomicColors.Gray.Transparent,
                    pressedBorderColor = YGAtomicColors.Gray.Transparent,
                    enabledIconColor = YGAtomicColors.Gray.Gray900,
                    disabledIconColor = YGAtomicColors.Gray.Gray900,
                    pressedIconColor = YGAtomicColors.Gray.Gray500,
                )
        }

        object Transparency : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

            override val radius: Shape
                @Composable
                get() = YGTheme.shapes.radius.round

            override val textStyle: TextStyle
                @Composable
                get() = YGTheme.typography.body.b01SB

            override val iconSize: Dp
                get() = SizeTokens.Size24.getDp()

            override val startPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val topPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val endPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding4

            override val bottomPadding: Dp
                @Composable
                get() = YGTheme.layout.padding.padding5

            override val colors: YGButtonColors
                @Composable
                get() = YGButtonColors(
                    enabledForegroundColor = YGAtomicColors.Gray.Gray900,
                    disabledForegroundColor = YGAtomicColors.Gray.Gray900,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                    enabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                    disabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.9f),
                    pressedBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                    enabledBorderColor = YGAtomicColors.Gray.Transparent,
                    disabledBorderColor = YGAtomicColors.Gray.Transparent,
                    pressedBorderColor = YGAtomicColors.Gray.Transparent,
                    enabledIconColor = YGAtomicColors.Gray.Gray900,
                    disabledIconColor = YGAtomicColors.Gray.Gray900,
                    pressedIconColor = YGAtomicColors.Gray.Gray500,
                )
        }
    }

    object Large : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap2

        override val radius: Shape
            @Composable
            get() = YGTheme.shapes.radius.round

        override val textStyle: TextStyle
            @Composable
            get() = YGTheme.typography.body.b01SB

        override val iconSize: Dp
            get() = SizeTokens.Size24.getDp()

        override val startPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding4

        override val topPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val endPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding4

        override val bottomPadding: Dp
            @Composable
            get() = YGTheme.layout.padding.padding5

        override val colors: YGButtonColors
            @Composable
            get() = YGButtonColors(
                enabledForegroundColor = YGAtomicColors.Gray.White,
                disabledForegroundColor = YGAtomicColors.Gray.White,
                pressedForegroundColor = YGAtomicColors.Gray.Gray500,
                enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                disabledBackgroundColor = YGAtomicColors.Gray.Gray950,
                pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                enabledBorderColor = YGAtomicColors.Gray.Transparent,
                disabledBorderColor = YGAtomicColors.Gray.Transparent,
                pressedBorderColor = YGAtomicColors.Gray.Transparent,
                enabledIconColor = YGAtomicColors.Gray.White,
                disabledIconColor = YGAtomicColors.Gray.White,
                pressedIconColor = YGAtomicColors.Gray.Gray500,
            )
    }
}
