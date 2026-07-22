package com.teamyg.parfait.feature.login.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.feature.login.api.NavKeyLogin
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.login.impl.route.LoginRoute
import com.teamyg.parfait.feature.login.impl.util.KakaoLoginHelper

fun EntryProviderScope<NavKey>.featureLoginEntryBuilder(
    navigator: Navigator,
    kakaoLoginHelper: KakaoLoginHelper,
) {
    entry<NavKeyLogin> {
        YGScaffold { innerPadding ->
            LoginRoute(
                navigator = navigator,
                kakaoLoginHelper = kakaoLoginHelper,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
