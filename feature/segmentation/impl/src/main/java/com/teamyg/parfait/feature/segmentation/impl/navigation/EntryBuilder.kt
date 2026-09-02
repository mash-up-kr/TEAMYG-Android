package com.teamyg.parfait.feature.segmentation.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.impl.route.SegmentationConfirmRoute
import com.teamyg.parfait.feature.segmentation.impl.route.SegmentationRoute
import com.teamyg.parfait.feature.segmentation.impl.route.ToppingEditRoute

fun EntryProviderScope<NavKey>.featureSegmentationEntryBuilder(navigator: Navigator) {
    entry<NavKeySegmentation> { key ->
        SegmentationRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeySegmentationConfirm> { key ->
        SegmentationConfirmRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<NavKeyToppingEdit> { key ->
        ToppingEditRoute(
            navigator = navigator,
            key = key,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
