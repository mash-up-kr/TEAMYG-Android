package com.teamyg.parfait.feature.groups.list.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.list.api.NavKeyGroupList
import com.teamyg.parfait.feature.groups.list.impl.route.GroupListRoute

fun EntryProviderScope<NavKey>.featureGroupListEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupList> {
        YGScaffold { innerPadding ->
            GroupListRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
