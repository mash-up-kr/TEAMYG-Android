package com.teamyg.parfait.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.preview.YGButtonPreviewData
import com.teamyg.parfait.core.designsystem.component.preview.YGButtonPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.token.GapTokens
import com.teamyg.parfait.core.designsystem.theme.token.PaddingTokens
import com.teamyg.parfait.core.designsystem.theme.token.ShapeTokens
import com.teamyg.parfait.core.designsystem.theme.token.SizeToken
import com.teamyg.parfait.core.designsystem.theme.token.SizeTokens
import com.teamyg.parfait.core.designsystem.theme.typography.YGTypography

@Composable
fun YGButton(
    text: String,
    buttonType: YGButtonType,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes startIconResource: Int? = null,
    @DrawableRes endIconResource: Int? = null,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = buttonType.colors.backgroundColor(
                    isEnabled = isEnabled,
                    isPressed = isPressed,
                ),
                shape = buttonType.radius,
            ).border(
                width = 1.dp,
                color = buttonType.colors.borderColor(
                    isEnabled = isEnabled,
                    isPressed = isPressed,
                ),
                shape = buttonType.radius,
            ).clip(shape = buttonType.radius)
            .clickable(enabled = isEnabled, onClick = onClick)
            .semantics { role = Role.Button }
            .padding(
                start = buttonType.startPadding.size.dp,
                end = buttonType.endPadding.size.dp,
                bottom = buttonType.bottomPadding.size.dp,
                top = buttonType.topPadding.size.dp,
            ),
    ) {
        startIconResource?.let { resource ->
            Image(
                painter = painterResource(resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = buttonType.colors.iconColor(
                        isEnabled = isEnabled,
                        isPressed = isPressed,
                    ),
                ),
            )
            Spacer(modifier = Modifier.width(buttonType.iconGapSize.size.dp))
        }
        Text(
            text = text,
            style = buttonType.textStyle,
            color = buttonType.colors.foregroundColor(
                isEnabled = isEnabled,
                isPressed = isPressed,
            ),
            textAlign = TextAlign.Center,
        )
        endIconResource?.let { resource ->
            Spacer(modifier = Modifier.width(buttonType.iconGapSize.size.dp))
            Image(
                painter = painterResource(id = resource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(
                    color = buttonType.colors.iconColor(
                        isEnabled = isEnabled,
                        isPressed = isPressed,
                    ),
                ),
            )
        }
    }
}

sealed interface YGButtonType {
    val iconGapSize: SizeToken
    val radius: Shape
    val textStyle: TextStyle
    val iconSize: SizeToken

    val startPadding: SizeToken
    val topPadding: SizeToken
    val endPadding: SizeToken
    val bottomPadding: SizeToken

    val colors: YgButtonColors

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

        override val colors: YgButtonColors = YgButtonColors(
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

        override val colors: YgButtonColors = YgButtonColors(
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

        override val colors: YgButtonColors = YgButtonColors(
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

            override val colors: YgButtonColors = YgButtonColors(
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

            override val colors: YgButtonColors = YgButtonColors(
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

            override val colors: YgButtonColors = YgButtonColors(
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

        override val colors: YgButtonColors = YgButtonColors(
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

class YgButtonColors(
    val enabledForegroundColor: Color,
    val disabledForegroundColor: Color,
    val pressedForegroundColor: Color,
    val enabledBackgroundColor: Color,
    val disabledBackgroundColor: Color,
    val pressedBackgroundColor: Color,
    val enabledBorderColor: Color,
    val disabledBorderColor: Color,
    val pressedBorderColor: Color,
    // Todo
    //  icon color 랑 foregroundColor 가 따라가야할 것 같은데 분리되어있어서 일단 분리하였음
    //  이 또한 문의 예정
    val enabledIconColor: Color,
    val disabledIconColor: Color,
    val pressedIconColor: Color,
) {
    fun foregroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledForegroundColor
        isPressed -> pressedForegroundColor
        else -> enabledForegroundColor
    }

    fun backgroundColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBackgroundColor
        isPressed -> pressedBackgroundColor
        else -> enabledBackgroundColor
    }

    fun borderColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledBorderColor
        isPressed -> pressedBorderColor
        else -> enabledBorderColor
    }

    fun iconColor(
        isEnabled: Boolean,
        isPressed: Boolean,
    ) = when {
        isEnabled.not() -> disabledIconColor
        isPressed -> pressedIconColor
        else -> enabledIconColor
    }
}

@Preview(showBackground = true)
@Composable
private fun YGButtonPreview(
    @PreviewParameter(YGButtonPreviewParameterProvider::class)
    data: YGButtonPreviewData,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text(data.name)
        YGButton(
            text = "Button Enabled",
            buttonType = data.buttonType,
            isEnabled = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
        YGButton(
            text = "Button Pressed",
            buttonType = data.buttonType,
            isEnabled = true,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
        YGButton(
            text = "Button Disabled",
            buttonType = data.buttonType,
            isEnabled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
        YGButton(
            text = "Button Start",
            buttonType = data.buttonType,
            isEnabled = true,
            startIconResource = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
        YGButton(
            text = "Button End",
            buttonType = data.buttonType,
            isEnabled = true,
            endIconResource = R.drawable.ic_plus,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        )
    }
}
