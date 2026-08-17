package com.teamyg.parfait.feature.common.terms.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.common.terms.api.NavKeyWebView
import com.teamyg.parfait.feature.common.terms.impl.route.WebViewRoute

fun EntryProviderScope<NavKey>.featureCommonTermsEntryBuilder(navigator: Navigator) {
    entry<NavKeyWebView> { key ->
        Scaffold { innerPadding ->
            WebViewRoute(
                title = key.title,
                url = key.url,
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
