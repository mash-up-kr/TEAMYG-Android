package com.teamyg.parfait.preview.navigation.entry

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.preview.navigation.key.NavKeyMain
import com.teamyg.parfait.preview.route.MainRoute

internal fun EntryProviderScope<NavKey>.featureMainEntryBuilder(navigator: Navigator) {
    entry<NavKeyMain> {
        Scaffold { innerPadding ->
            MainRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
