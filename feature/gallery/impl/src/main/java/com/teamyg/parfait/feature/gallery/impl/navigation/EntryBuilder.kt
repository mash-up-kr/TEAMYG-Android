package com.teamyg.parfait.feature.gallery.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.impl.route.CustomGalleryPickerRoute

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
