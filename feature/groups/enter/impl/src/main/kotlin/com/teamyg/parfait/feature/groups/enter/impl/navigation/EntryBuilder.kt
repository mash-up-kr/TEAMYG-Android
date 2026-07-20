package com.teamyg.parfait.feature.groups.enter.impl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.navigationBarsAndImePadding
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupNickName
import com.teamyg.parfait.feature.groups.enter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groups.enter.impl.groupcreate.GroupCreateRoute
import com.teamyg.parfait.feature.groups.enter.impl.invitecode.GroupInviteCodeRoute
import com.teamyg.parfait.feature.groups.enter.impl.nickname.GroupNickNameRoute

fun EntryProviderScope<NavKey>.featureGroupInviteCodeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupInviteCode> {
        YGScaffold(
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            GroupInviteCodeRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Gray.White)
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsAndImePadding(),
            )
        }
    }
}

fun EntryProviderScope<NavKey>.featureGroupNickNameEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupNickName> { _ ->
        YGScaffold(
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            GroupNickNameRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .background(YGAtomicColors.Gray.White)
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsAndImePadding(),
            )
        }
    }
}

fun EntryProviderScope<NavKey>.featureGroupCreateEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupInviteCode> { _ ->
        YGScaffold(
            contentWindowInsets = WindowInsets(0.dp),
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            GroupCreateRoute(
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
