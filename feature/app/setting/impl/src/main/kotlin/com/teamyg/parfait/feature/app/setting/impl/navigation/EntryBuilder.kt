package com.teamyg.parfait.feature.app.setting.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.app.setting.impl.route.AppSettingRoute

fun EntryProviderScope<NavKey>.featureAppSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyAppSetting> {
        Scaffold { innerPadding ->
            AppSettingRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
