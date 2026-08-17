package com.teamyg.parfait.feature.intro.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.domain.model.auth.RegistrationToken
import com.teamyg.parfait.feature.intro.api.NavKeySplash
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.intro.api.NavKeyTermAgree
import com.teamyg.parfait.feature.intro.impl.splash.SplashRoute
import com.teamyg.parfait.feature.intro.impl.termagree.TermAgreeRoute

fun EntryProviderScope<NavKey>.featureSplashEntryBuilder(navigator: Navigator) {
    entry<NavKeySplash> {
        SplashRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun EntryProviderScope<NavKey>.featureTermAgreeEntryBuilder(navigator: Navigator) {
    entry<NavKeyTermAgree> { navKey ->
        YGScaffold { innerPadding ->
            TermAgreeRoute(
                navigator = navigator,
                registrationToken = RegistrationToken(navKey.registrationToken),
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = YGTheme.colorScheme.grayScale.white)
                    .padding(innerPadding),
            )
        }
    }
}
