package com.teamyg.parfait.feature.groupenter.impl

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groupenter.api.NavKeyGroupInviteCode
import com.teamyg.parfait.feature.groupenter.impl.invitecode.GroupInviteCodeRoute

fun EntryProviderScope<NavKey>.featureGroupInviteCodeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupInviteCode> { _ ->
        val navigationBarsAndImePadding = navigationBarsAndImePadding()

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
                    .windowInsetsPadding(navigationBarsAndImePadding),
            )
        }
    }
}

@Composable
fun navigationBarsAndImePadding(): WindowInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val navigationBarBottomPadding = WindowInsets.navigationBars
    val imeBottomPadding = WindowInsets.ime

    return WindowInsets(
        left = maxOf(
            a = navigationBarBottomPadding.getLeft(density, layoutDirection),
            b = imeBottomPadding.getLeft(density, layoutDirection),
        ),
        right = maxOf(
            a = navigationBarBottomPadding.getRight(density, layoutDirection),
            b = imeBottomPadding.getRight(density, layoutDirection),
        ),
        top = maxOf(
            a = navigationBarBottomPadding.getTop(density),
            b = imeBottomPadding.getTop(density),
        ),
        bottom = maxOf(
            a = navigationBarBottomPadding.getBottom(density),
            b = imeBottomPadding.getBottom(density),
        ),
    )
}
