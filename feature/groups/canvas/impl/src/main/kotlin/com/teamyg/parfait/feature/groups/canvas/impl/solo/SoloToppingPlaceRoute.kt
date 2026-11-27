package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitEdit
import com.teamyg.parfait.core.util.android.extension.triggerSnapHaptic
import com.teamyg.parfait.feature.groups.canvas.api.CanvasHomeNavKey
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingPlaceScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingPlaceIntent

/** 혼자 공간 토핑 배치. 화면은 `CanvasToppingPlaceScreen`을 쓰고, 저장 경로만 `SoloToppingPlaceViewModel`이 다르다 */
@Composable
internal fun SoloToppingPlaceRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SoloToppingPlaceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val context = LocalContext.current
    val draftUnavailableMessage = stringResource(R.string.canvas_topping_place_draft_unavailable)
    val placeFailedMessage = stringResource(R.string.solo_topping_place_failure)
    val toppingImageNotReadyMessage = stringResource(R.string.canvas_topping_place_image_not_ready)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SoloToppingPlaceEffect.NavigateBack -> navigator.onBack()

                // 캔버스를 새로 쌓지 않고 원래 자리로 되감는다. 새로 쌓으면 방금 끝난 토핑 만들기 화면들이
                // 그 밑에 남고, 다음 흐름이 진입하며 비우는 세그멘테이션 캐시가 그 화면들이 가리키던
                // PNG 를 지운다(뒤로 가면 빈 이미지만 남는다)
                SoloToppingPlaceEffect.QuitToCanvas,
                SoloToppingPlaceEffect.PlaceSucceeded,
                -> navigator.popUpTo<CanvasHomeNavKey>()

                // 알리고 화면엔 남는다 — 되감으면 이 Route 의 toastPolicy 까지 폐기돼 안내가 잔상으로 끝난다
                SoloToppingPlaceEffect.DraftMissing -> toastPolicy.showError(draftUnavailableMessage)

                SoloToppingPlaceEffect.PlaceFailed -> toastPolicy.showError(placeFailedMessage)

                SoloToppingPlaceEffect.ToppingImageNotReady -> toastPolicy.showError(toppingImageNotReadyMessage)

                SoloToppingPlaceEffect.TriggerSnapHaptic -> context.triggerSnapHaptic()
            }
        }
    }

    BackHandler { viewModel.processIntent(CanvasToppingPlaceIntent.OnSystemBack) }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        CanvasToppingPlaceScreen(
            uiState = uiState,
            onClickBack = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickBack) },
            onClickClose = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickClose) },
            onClickConfirm = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickConfirm) },
            onClickTopping = { viewModel.processIntent(CanvasToppingPlaceIntent.OnClickTopping) },
            onToggleBorderPanel = { viewModel.processIntent(CanvasToppingPlaceIntent.OnToggleBorderPanel) },
            onDismissBorderPanel = { viewModel.processIntent(CanvasToppingPlaceIntent.OnDismissBorderPanel) },
            onSelectBorderColor = { colorArgb ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnSelectBorderColor(colorArgb))
            },
            onChangeBorderWidth = { widthDp ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnChangeBorderWidth(widthDp))
            },
            onToppingTransform = { pan, zoom, rotationDelta ->
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingTransform(pan, zoom, rotationDelta))
            },
            onToppingTransformGestureEnd = {
                viewModel.processIntent(CanvasToppingPlaceIntent.OnToppingTransformGestureEnd)
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

    if (uiState.showQuitDialog) {
        YGModalQuitEdit(
            onConfirmQuit = { viewModel.processIntent(CanvasToppingPlaceIntent.OnQuitDialogConfirm) },
            onDismiss = { viewModel.processIntent(CanvasToppingPlaceIntent.OnQuitDialogCancel) },
        )
    }
}
