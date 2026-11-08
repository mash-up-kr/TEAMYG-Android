package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun ListItemToggle(
    title: String,
    subText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YGTheme.layout.padding.padding5),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = if (enabled) YGTheme.typography.body.b02SB else YGTheme.typography.body.b02R,
                color = if (enabled) YGAtomicColors.Gray.Gray800 else YGAtomicColors.Gray.Gray400,
            )
            Text(
                text = subText,
                style = YGTheme.typography.caption.c01R,
                color = YGAtomicColors.Gray.Gray400,
            )
        }

        NotificationSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@YGPreview
@Composable
private fun ListItemTogglePreview() = PreviewBox {
    Column {
        ListItemToggle(
            title = "푸시 알림",
            subText = "새 캔버스와 그룹 소식을 알려드려요",
            checked = true,
            enabled = true,
            onCheckedChange = {},
        )
        ListItemToggle(
            title = "푸시 알림",
            subText = "새 캔버스와 그룹 소식을 알려드려요",
            checked = false,
            enabled = true,
            onCheckedChange = {},
        )
        ListItemToggle(
            title = "푸시 알림",
            subText = "새 캔버스와 그룹 소식을 알려드려요",
            checked = true,
            enabled = false,
            onCheckedChange = {},
        )
        ListItemToggle(
            title = "푸시 알림",
            subText = "새 캔버스와 그룹 소식을 알려드려요",
            checked = false,
            enabled = false,
            onCheckedChange = {},
        )
    }
}
