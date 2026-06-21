package com.teamyg.parfait.feature.groupenter.impl

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.extensions.navigationBarsAndImePadding
import com.teamyg.parfait.feature.groupenter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groupenter.impl.invitecode.GroupInviteCodeRoute

fun EntryProviderScope<NavKey>.featureGroupInviteCodeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupInviteCode> { _ ->
        Scaffold(
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            GroupInviteCodeRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsAndImePadding(),
            )
        }
    }
}
