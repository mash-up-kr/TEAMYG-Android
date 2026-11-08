package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private val TrackWidth = 60.dp
private val ThumbSize = 24.dp
private val ThumbVerticalInset = 4.dp
private val ThumbHorizontalInset = 5.dp
private val BorderWidth = 1.dp

@Composable
internal fun NotificationSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbHorizontalInset * 2 else 0.dp,
        label = "NotificationSwitchThumbOffset",
    )

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .width(width = TrackWidth)
            .background(
                color = if (enabled) YGAtomicColors.Gray.White else YGAtomicColors.Gray.Gray50,
            ).border(
                width = BorderWidth,
                color = borderColor(checked = checked, enabled = enabled),
            ).toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = null,
                indication = null,
                onValueChange = onCheckedChange,
            ).padding(
                vertical = ThumbVerticalInset,
                horizontal = ThumbHorizontalInset,
            ),
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffset.roundToPx(), y = 0) }
                .size(ThumbSize)
                .background(
                    color = thumbColor(checked = checked, enabled = enabled),
                ),
        )
    }
}

private fun thumbColor(
    checked: Boolean,
    enabled: Boolean,
) = when {
    !enabled -> YGAtomicColors.Gray.Gray200
    checked -> YGAtomicColors.Gray.Gray900
    else -> YGAtomicColors.Gray.Gray300
}

private fun borderColor(
    checked: Boolean,
    enabled: Boolean,
) = if (checked && enabled) YGAtomicColors.Cherry.Cherry200 else YGAtomicColors.Gray.Gray200

@YGPreview
@Composable
private fun NotificationSwitchPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
        NotificationSwitch(checked = true, enabled = true, onCheckedChange = {})
        NotificationSwitch(checked = false, enabled = true, onCheckedChange = {})
        NotificationSwitch(checked = true, enabled = false, onCheckedChange = {})
        NotificationSwitch(checked = false, enabled = false, onCheckedChange = {})
    }
}
