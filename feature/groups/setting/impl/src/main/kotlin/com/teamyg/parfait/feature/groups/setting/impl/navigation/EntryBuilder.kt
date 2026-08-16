package com.teamyg.parfait.feature.groups.setting.impl.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.feature.groups.setting.impl.route.GroupSettingRoute

fun EntryProviderScope<NavKey>.featureGroupSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupSetting> { navKey ->
        YGScaffold { innerPadding ->
            GroupSettingRoute(
                groupId = navKey.groupId,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Scaffold가 준 systemBars 인셋을 소비해, 하위 imePadding()이
                    // 내비게이션 바 높이를 두 번 세지 않게 한다.
                    .consumeWindowInsets(innerPadding),
            )
        }
    }
}
