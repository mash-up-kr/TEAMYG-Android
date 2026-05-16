package com.teamyg.grouphome.impl

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.grouphome.api.NavKeyGroupHome
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureGroupHomeEntryBuilder(navigator: Navigator) {
    entry<NavKeyGroupHome> { key ->
        Scaffold { innerPadding ->
            GroupHomeRoute(
                navigator = navigator,
                key = key,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
