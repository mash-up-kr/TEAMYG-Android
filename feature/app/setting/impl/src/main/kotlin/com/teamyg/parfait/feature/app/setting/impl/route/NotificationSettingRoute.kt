package com.teamyg.parfait.feature.app.setting.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.impl.screen.NotificationSettingScreen

@Composable
internal fun NotificationSettingRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    YGScaffoldV2(modifier = modifier) { innerPadding ->
        NotificationSettingScreen(
            onClickBack = navigator::onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
