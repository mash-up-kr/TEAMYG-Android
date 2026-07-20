package com.teamyg.parfait.feature.app.setting.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.app.setting.api.NavKeyAccountInfo
import com.teamyg.parfait.feature.app.setting.api.NavKeyAppSetting
import com.teamyg.parfait.feature.app.setting.api.NavKeyPrivacyPolicy
import com.teamyg.parfait.feature.app.setting.api.NavKeyServiceTerms
import com.teamyg.parfait.feature.app.setting.impl.route.AccountInfoRoute
import com.teamyg.parfait.feature.app.setting.impl.route.AppSettingRoute
import com.teamyg.parfait.feature.app.setting.impl.route.PrivacyPolicyRoute
import com.teamyg.parfait.feature.app.setting.impl.route.ServiceTermsRoute

fun EntryProviderScope<NavKey>.featureAppSettingEntryBuilder(navigator: Navigator) {
    entry<NavKeyAppSetting> {
        Scaffold(containerColor = YGAtomicColors.Gray.White) { innerPadding ->
            AppSettingRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

    entry<NavKeyAccountInfo> {
        Scaffold { innerPadding ->
            AccountInfoRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }

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
