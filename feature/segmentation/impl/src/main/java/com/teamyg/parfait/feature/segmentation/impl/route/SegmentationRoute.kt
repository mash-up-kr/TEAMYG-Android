package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen

@Composable
internal fun SegmentationRoute(
    navigator: Navigator,
    key: NavKeySegmentation,
    modifier: Modifier = Modifier,
) {
    SegmentationScreen(
        sourceImageUri = key.sourceImageUri,
        modifier = modifier,
        onClickBack = { navigator.onBack() },
    )
}
