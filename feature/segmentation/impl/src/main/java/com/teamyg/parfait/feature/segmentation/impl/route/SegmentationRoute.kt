package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationViewModel

@Composable
internal fun SegmentationRoute(
    navigator: Navigator,
    key: NavKeySegmentation,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationViewModel, SegmentationViewModel.Factory>(
        creationCallback = { factory -> factory.create(key.sourceImageUri) },
    )
    SegmentationScreen(
        viewModel = viewModel,
        modifier = modifier,
        onClickBack = { navigator.onBack() },
        onClickOk = { uri -> navigator.goTo(NavKeyCanvasMove(imageUri = uri)) },
    )
}
