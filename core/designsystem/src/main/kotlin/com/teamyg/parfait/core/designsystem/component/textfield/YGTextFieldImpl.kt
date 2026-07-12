package com.teamyg.parfait.core.designsystem.component.textfield

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens

@Composable
internal fun YGTextFieldImpl(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    enabled: Boolean,
    isError: Boolean,
    maxLength: Int?,
    colors: YGTextFieldColors,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val isFocused: Boolean by interactionSource.collectIsFocusedAsState()

    val showCounter = maxLength != null && value.isNotEmpty()
    val showClear = enabled && value.isNotEmpty()

    val commonShape: Shape = YGTheme.shapes.radius.small

    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap5),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = colors.backgroundColor(isEnabled = enabled),
                shape = commonShape,
            ).border(
                width = SizeTokens.Size1.getDp(),
                color = colors.borderColor(
                    isEnabled = enabled,
                    isFocused = isFocused,
                    isError = isError,
                ),
                shape = commonShape,
            ).clip(
                shape = commonShape,
            ).padding(
                start = YGTheme.layout.padding.padding6,
                top = if (showClear) YGTheme.layout.padding.padding1 else YGTheme.layout.padding.padding5,
                end = if (showClear) YGTheme.layout.padding.padding2 else YGTheme.layout.padding.padding6,
                bottom = if (showClear) YGTheme.layout.padding.padding1 else YGTheme.layout.padding.padding5,
            ),
    ) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (maxLength == null || newValue.length <= maxLength) {
                    onValueChange(newValue)
                }
            },
            modifier = Modifier.weight(1f),
            enabled = enabled,
            textStyle = YGTheme.typography.body.b01R.copy(
                color = colors.textColor(isEnabled = enabled),
            ),
            cursorBrush = SolidColor(colors.cursorColor),
            singleLine = true,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = YGTheme.typography.body.b01R,
                        color = colors.placeholderColor,
                    )
                }
                innerTextField()
            },
        )

        if (showCounter || showClear) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showCounter) {
                    Text(
                        text = "${value.length}/$maxLength",
                        style = if (isError) YGTheme.typography.body.b02SB else YGTheme.typography.body.b02R,
                        color = colors.counterColor(isError = isError),
                    )
                }

                if (showClear) {
                    // TODO Change IconButton
                    Box(
                        modifier = Modifier
                            .clickable(role = Role.Button) { onValueChange("") }
                            .size(SizeTokens.Size44.getDp()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_close_round),
                            contentDescription = "clear",
                            colorFilter = ColorFilter.tint(colors.clearIconTint),
                            modifier = Modifier.size(SizeTokens.Size24.getDp()),
                        )
                    }
                }
            }
        }
    }
}
