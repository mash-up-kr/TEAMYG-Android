package com.teamyg.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.navigation.Navigator
import com.teamyg.segmentation.api.NavKeySegmentation
import com.teamyg.segmentation.impl.screen.SegmentationScreen

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
