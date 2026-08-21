package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingPlaceScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceViewModel

@Composable
internal fun CanvasToppingPlaceRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasToppingPlaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasToppingPlaceEffect.NavigateBack -> navigator.onBack()

                // 알리고 화면엔 남는다 — 여기서 되감으면 이 Route에 매달린 toastPolicy까지 같이
                // 폐기돼 안내가 잔상으로 끝난다. 닫기 버튼이 이미 있어 막다른 곳도 아니다
                CanvasToppingPlaceEffect.DraftMissing -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_draft_unavailable))
                }

                // 캔버스를 새로 쌓지 않고 원래 자리로 되감는다. 새로 쌓으면 방금 끝난 토핑 만들기
                // 화면들이 그 밑에 남고, 다음 흐름이 진입하며 비우는 세그멘테이션 캐시가 그 화면들이
                // 가리키던 PNG 를 지운다(뒤로 가면 빈 이미지만 남는다)
                CanvasToppingPlaceEffect.PlaceSucceeded -> navigator.popUpTo<NavKeyCanvasMain>()

                CanvasToppingPlaceEffect.PlaceFailed -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_failed))
                }

                // 다시 눌러도 같은 실패라 잡아 두지 않는다. 되감아도 막 만든 토핑은 초안에 남는다
                CanvasToppingPlaceEffect.PlaceFailedPermanently -> {
                    toastPolicy.showError(context.getString(R.string.canvas_topping_place_failed_permanently))
                    navigator.popUpTo<NavKeyCanvasMain>()
                }
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
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
            onToppingImageReadyChanged = { isReady ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingImageReadyChanged(isReady))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
