package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R

@Composable
internal fun NotificationInfoCard(
    onClickOpenDeviceSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = YGAtomicColors.Transparency.White75)
            .border(width = 1.dp, color = YGAtomicColors.Cherry.Cherry100)
            .padding(YGTheme.layout.padding.padding6),
    ) {
        Text(
            text = stringResource(R.string.notification_setting_device_off_title),
            style = YGTheme.typography.body.b02SB,
            color = YGAtomicColors.Gray.Gray900,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap2))
        Text(
            text = stringResource(R.string.notification_setting_device_off_body),
            style = YGTheme.typography.caption.c01R,
            color = YGAtomicColors.Gray.Gray500,
        )
        Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap4))
        YGButton(
            text = stringResource(R.string.notification_setting_open_device_settings),
            buttonType = YGButtonType.SmallSquare,
            isEnabled = true,
            onClick = onClickOpenDeviceSettings,
        )
    }
}

@YGPreview
@Composable
private fun NotificationInfoCardPreview() = PreviewBox {
    NotificationInfoCard(onClickOpenDeviceSettings = {})
}
