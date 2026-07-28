package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

sealed interface YGButtonType {
    @get:Composable
    val iconGapSize: Dp

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

    object SmallSquare : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap1

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
                enabledForegroundColor = YGAtomicColors.Gray.White,
                disabledForegroundColor = YGAtomicColors.Gray.Gray400,
                pressedForegroundColor = YGAtomicColors.Gray.White,
                enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                pressedBackgroundColor = YGAtomicColors.Gray.Black,
            )
    }

    object Medium {
        object Primary : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

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
                    disabledForegroundColor = YGAtomicColors.Gray.Gray500,
                    pressedForegroundColor = YGAtomicColors.Gray.White,
                    enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                    disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                    pressedBackgroundColor = YGAtomicColors.Gray.Gray950,
                )
        }

        object Secondary : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

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
                    disabledForegroundColor = YGAtomicColors.Gray.Gray500,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray900,
                    enabledBackgroundColor = YGAtomicColors.Gray.Gray100,
                    disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                    pressedBackgroundColor = YGAtomicColors.Gray.Gray200,
                )
        }

        object Transparency : YGButtonType {
            override val iconGapSize: Dp
                @Composable
                get() = YGTheme.layout.gap.gap2

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
                    disabledForegroundColor = YGAtomicColors.Gray.Gray500,
                    pressedForegroundColor = YGAtomicColors.Gray.Gray900,
                    enabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                    disabledBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.5f),
                    pressedBackgroundColor = YGAtomicColors.Gray.White.copy(alpha = 0.9f),
                )
        }
    }

    object Large : YGButtonType {
        override val iconGapSize: Dp
            @Composable
            get() = YGTheme.layout.gap.gap2

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
                disabledForegroundColor = YGAtomicColors.Gray.Gray500,
                pressedForegroundColor = YGAtomicColors.Gray.White,
                enabledBackgroundColor = YGAtomicColors.Gray.Gray900,
                disabledBackgroundColor = YGAtomicColors.Gray.Gray200,
                pressedBackgroundColor = YGAtomicColors.Gray.Gray950,
            )
    }
}
