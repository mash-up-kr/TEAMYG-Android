package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingPlaceScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceViewModel

@Composable
internal fun CanvasToppingPlaceRoute(
    navigator: Navigator,
    key: NavKeyCanvasToppingPlace,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<CanvasToppingPlaceViewModel, CanvasToppingPlaceViewModel.Factory>(
        creationCallback = { factory -> factory.create(imageUri = key.imageUri) },
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasToppingPlaceEffect.NavigateBack -> navigator.onBack()

                is CanvasToppingPlaceEffect.ToppingPlaced -> {
                    // TODO: 배치 결과(effect)를 캔버스 상태에 반영/서버에 저장하는 연동 필요
                    navigator.goTo(NavKeyCanvasMain(groupId = 0L))
                }
            }
        }
    }

    YGScaffoldV2(modifier = modifier) { innerPadding ->
        CanvasToppingPlaceScreen(
            uiState = uiState,
            onClickClose = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickClose) },
            onClickConfirm = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm) },
            onToppingMoveDrag = { delta -> viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingMoveDrag(delta)) },
            onToppingResizeDrag = { delta ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingResizeDrag(delta))
            },
            onToppingRotateDrag = { delta ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingRotateDrag(delta))
            },
            onCanvasMeasured = { size -> viewModel.processIntent(CanvasToppingPlaceIntent.OnCanvasMeasured(size)) },
            onToppingBaseSizeMeasured = { size ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingBaseSizeMeasured(size))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
