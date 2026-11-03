package com.teamyg.parfait.feature.app.setting.impl.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDetail
import com.teamyg.parfait.core.designsystem.screen.YGScreen
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R

@Composable
internal fun NotificationSettingScreen(
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YGScreen(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            YGTopBarDetail(
                title = stringResource(R.string.notification_setting_title),
                onIconClick = onClickBack,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        OnBack { onClickBack() }
    }
}

@YGPreview
@Composable
private fun NotificationSettingScreenPreview() = PreviewBox {
    NotificationSettingScreen(
        onClickBack = {},
        modifier = Modifier.fillMaxSize(),
    )
}
