package com.teamyg.parfait.feature.intro.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.feature.intro.impl.splash.SplashRoute
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeRoute

fun EntryProviderScope<NavKey>.featureSplashEntryBuilder(navigator: Navigator) {
    entry<NavKeySplash> {
        YGScaffold { innerPadding ->
            SplashRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

fun EntryProviderScope<NavKey>.featureTermAgreeEntryBuilder(navigator: Navigator) {
    entry<NavKeyTermAgree> {
        Scaffold { innerPadding ->
            TermAgreeRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = YGTheme.colorScheme.grayScale.white)
                    .padding(innerPadding),
            )
        }
    }
}
