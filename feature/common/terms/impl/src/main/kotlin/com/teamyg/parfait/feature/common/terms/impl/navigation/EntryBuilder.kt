package com.teamyg.parfait.feature.common.terms.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.common.terms.api.NavKeyPrivacyPolicy
import com.teamyg.parfait.feature.common.terms.api.NavKeyServiceTerms
import com.teamyg.parfait.feature.common.terms.impl.route.PrivacyPolicyRoute
import com.teamyg.parfait.feature.common.terms.impl.route.ServiceTermsRoute

fun EntryProviderScope<NavKey>.featureCommonTermsEntryBuilder(navigator: Navigator) {
    entry<NavKeyServiceTerms> {
        Scaffold { innerPadding ->
            ServiceTermsRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyPrivacyPolicy> {
        Scaffold { innerPadding ->
            PrivacyPolicyRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
