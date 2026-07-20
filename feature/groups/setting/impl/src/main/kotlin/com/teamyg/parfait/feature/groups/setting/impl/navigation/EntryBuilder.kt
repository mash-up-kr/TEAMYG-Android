package com.teamyg.parfait.feature.groups.setting.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.feature.groups.setting.impl.route.GroupSettingRoute

fun EntryProviderScope<NavKey>.featureGroupSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupSetting> {
        YGScaffold { innerPadding ->
            GroupSettingRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
