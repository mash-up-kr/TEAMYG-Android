package com.teamyg.parfait.feature.gallery.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.NavKeySystemGalleryPicker
import com.teamyg.parfait.feature.gallery.impl.route.CustomGalleryPickerRoute
import com.teamyg.parfait.feature.gallery.impl.route.SystemGalleryPickerRoute

fun EntryProviderScope<NavKey>.featureSystemGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeySystemGalleryPicker> {
        SystemGalleryPickerRoute(
            navigator = navigator,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

fun EntryProviderScope<NavKey>.featureCustomGalleryEntryBuilder(navigator: Navigator) {
    entry<NavKeyCustomGalleryPicker> { navKey ->
        CustomGalleryPickerRoute(
            navigator = navigator,
            recentImagePick = navKey.recentImagePick,
            showGuideToast = navKey.showGuideToast,
            returnResultOnly = navKey.returnResultOnly,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
