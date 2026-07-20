package com.teamyg.parfait.feature.groups.home.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.feature.groups.home.api.NavKeyGroupHome
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.home.impl.route.GroupHomeRoute

fun EntryProviderScope<NavKey>.featureGroupHomeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupHome> { key ->
        YGScaffold { innerPadding ->
            GroupHomeRoute(
                navigator = navigator,
                key = key,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
