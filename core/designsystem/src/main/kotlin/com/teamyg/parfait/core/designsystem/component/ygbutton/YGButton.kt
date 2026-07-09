package com.teamyg.parfait.core.designsystem.component.ygbutton

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme

@Composable
fun YGButton(
    text: String,
    buttonType: YGButtonType,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes startIconResource: Int? = null,
    @DrawableRes endIconResource: Int? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isPressed: Boolean by interactionSource.collectIsPressedAsState()

    YGButton(
        text = text,
        buttonType = buttonType,
        isEnabled = isEnabled,
        isPressed = isPressed,
        modifier = modifier
            .clickable(enabled = isEnabled, onClick = onClick, interactionSource = interactionSource)
            .semantics { role = Role.Button },
        startIconResource = startIconResource,
        endIconResource = endIconResource,
    )
}

@Composable
private fun YGButton(
    text: String,
    buttonType: YGButtonType,
    isEnabled: Boolean,
    isPressed: Boolean,
    modifier: Modifier = Modifier,
    @DrawableRes startIconResource: Int? = null,
    @DrawableRes endIconResource: Int? = null,
) {
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
            .padding(
                start = buttonType.startPadding,
                top = buttonType.topPadding,
                end = buttonType.endPadding,
                bottom = buttonType.bottomPadding,
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
            Spacer(modifier = Modifier.width(buttonType.iconGapSize))
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
            Spacer(modifier = Modifier.width(buttonType.iconGapSize))
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

@Preview(showBackground = true)
@Composable
private fun YGButtonPreview(
    @PreviewParameter(YGButtonPreviewParameterProvider::class)
    data: YGButtonPreviewData,
) {
    YGCustomTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(data.name)
            YGButton(
                text = "Button Enabled",
                buttonType = data.buttonType,
                isEnabled = true,
                isPressed = false,
                modifier = Modifier.fillMaxWidth(),
            )
            YGButton(
                text = "Button Pressed",
                buttonType = data.buttonType,
                isEnabled = true,
                isPressed = true,
                modifier = Modifier.fillMaxWidth(),
            )
            YGButton(
                text = "Button Disabled",
                buttonType = data.buttonType,
                isEnabled = false,
                isPressed = false,
                modifier = Modifier.fillMaxWidth(),
            )
            YGButton(
                text = "Button Start",
                buttonType = data.buttonType,
                isEnabled = true,
                isPressed = false,
                startIconResource = R.drawable.ic_plus,
                modifier = Modifier.fillMaxWidth(),
            )
            YGButton(
                text = "Button End",
                buttonType = data.buttonType,
                isEnabled = true,
                isPressed = false,
                endIconResource = R.drawable.ic_plus,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
