package com.teamyg.parfait.feature.gallery.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.designsystem.screen.YGScaffold
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.gallery.impl.route.CustomGalleryPickerRoute
import com.teamyg.parfait.feature.gallery.impl.route.SystemGalleryPickerRoute

fun EntryProviderScope<NavKey>.featureSystemGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeySystemGalleryPicker> {
        YGScaffold { innerPadding ->
            SystemGalleryPickerRoute(
                navigator = navigator,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

fun EntryProviderScope<NavKey>.featureCustomGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeyCustomGalleryPicker> { navKey ->
        YGScaffold { innerPadding ->
            CustomGalleryPickerRoute(
                navigator = navigator,
                showGuideToast = navKey.showGuideToast,
                returnResultOnly = navKey.returnResultOnly,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
