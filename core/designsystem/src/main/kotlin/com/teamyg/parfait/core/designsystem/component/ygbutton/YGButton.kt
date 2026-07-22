package com.teamyg.parfait.core.designsystem.component.ygbutton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

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
            ).clip(shape = buttonType.radius)
            .clickable(
                enabled = isEnabled,
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null,
            ).semantics { role = Role.Button }
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
                    color = buttonType.colors.foregroundColor(
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
                    color = buttonType.colors.foregroundColor(
                        isEnabled = isEnabled,
                        isPressed = isPressed,
                    ),
                ),
            )
        }
    }
}

@YGPreview
@Composable
private fun YGButtonPreview(
    @PreviewParameter(YGButtonPreviewParameterProvider::class)
    data: YGButtonPreviewData,
) = PreviewBox {
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
