package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.canvas.api.NavKeyCanvasMove
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEffect
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
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect){
                is SegmentationEffect.SegmentationFailed -> {
                    Toast.makeText(context, "객체 감지 실패", Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }
            }
        }
    }

    SegmentationScreen(
        state = state,
        modifier = modifier,
        onClickBack = { navigator.onBack() },
        onClickOk = { uri -> navigator.goTo(NavKeyCanvasMove(imageUri = uri)) },
    )
}
