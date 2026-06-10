package com.teamyg.parfait.feature.segmentation.impl.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.impl.route.SegmentationRoute

fun EntryProviderScope<NavKey>.featureSegmentationEntryBuilder(navigator: Navigator) {
    entry<NavKeySegmentation> { key ->
        Scaffold { innerPadding ->
            SegmentationRoute(
                navigator = navigator,
                key = key,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}
