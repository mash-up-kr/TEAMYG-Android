package com.teamyg.parfait.feature.intro.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
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
        TermAgreeRoute(
            navigator = navigator,
            registrationToken = RegistrationToken(navKey.registrationToken),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
