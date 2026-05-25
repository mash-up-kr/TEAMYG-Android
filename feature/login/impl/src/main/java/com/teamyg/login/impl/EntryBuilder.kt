package com.teamyg.login.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.login.api.NavigationKey
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureLoginEntryBuilder(navigator: Navigator) {
    entry<NavigationKey> {
        Scaffold { innerPadding ->
            LoginRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
