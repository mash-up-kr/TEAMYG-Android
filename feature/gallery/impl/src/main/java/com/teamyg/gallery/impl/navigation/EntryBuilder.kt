package com.teamyg.gallery.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.gallery.impl.route.SystemGalleryPickerRoute
import com.teamyg.navigation.Navigator

fun EntryProviderScope<NavKey>.featureSystemGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeySystemGalleryPicker> {
        Scaffold { innerPadding ->
            SystemGalleryPickerRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
