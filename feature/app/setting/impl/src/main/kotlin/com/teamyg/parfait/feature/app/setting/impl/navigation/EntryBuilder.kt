package com.teamyg.parfait.feature.app.setting.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.app.setting.impl.route.AccountInfoRoute
import com.teamyg.parfait.feature.app.setting.impl.route.AppSettingRoute

fun EntryProviderScope<NavKey>.featureAppSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyAppSetting> {
        YGScaffold { innerPadding ->
            AppSettingRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyAccountInfo> {
        YGScaffold { innerPadding ->
            AccountInfoRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
