package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButton
import com.teamyg.parfait.core.designsystem.component.ygbutton.YGButtonType
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.feature.app.setting.impl.component.ListItemToggle
import com.teamyg.parfait.feature.app.setting.impl.viewmodel.NotificationSettingState

@Composable
internal fun NotificationSettingScreen(
    state: NotificationSettingState,
    isNotificationEnabled: Boolean,
    onClickBack: () -> Unit,
    onClickOpenDeviceSettings: () -> Unit,
    onChangeCanvasNotification: (Boolean) -> Unit,
    onChangeActivityNotification: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.notification_setting_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = YGTheme.layout.padding.padding7),
            ) {
                if (!isNotificationEnabled) {
                    NotificationInfoCard(
                        onClickOpenDeviceSettings = onClickOpenDeviceSettings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap8))
                }

                Column(verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3)) {
                    ListItemToggle(
                        title = stringResource(R.string.notification_setting_canvas_title),
                        subText = stringResource(R.string.notification_setting_canvas_sub),
                        checked = state.isCanvasNotificationChecked,
                        enabled = isNotificationEnabled,
                        onCheckedChange = onChangeCanvasNotification,
                    )
                    ListItemToggle(
                        title = stringResource(R.string.notification_setting_activity_title),
                        subText = stringResource(R.string.notification_setting_activity_sub),
                        checked = state.isActivityNotificationChecked,
                        enabled = isNotificationEnabled,
                        onCheckedChange = onChangeActivityNotification,
                    )
                }
            }
        }

        OnBack { onClickBack() }
    }
}

@Composable
private fun NotificationInfoCard(
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
private fun NotificationSettingScreenPreview() = PreviewBox {
    Column {
        NotificationSettingScreen(
            state = NotificationSettingState(),
            isNotificationEnabled = false,
            onClickBack = {},
            onClickOpenDeviceSettings = {},
            onChangeCanvasNotification = {},
            onChangeActivityNotification = {},
        )
        NotificationSettingScreen(
            state = NotificationSettingState(),
            isNotificationEnabled = true,
            onClickBack = {},
            onClickOpenDeviceSettings = {},
            onChangeCanvasNotification = {},
            onChangeActivityNotification = {},
        )
    }
}
