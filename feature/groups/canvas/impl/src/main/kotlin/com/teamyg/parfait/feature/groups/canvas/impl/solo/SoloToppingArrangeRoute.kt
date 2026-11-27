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
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasToppingArrangeScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasToppingArrangeIntent

/** 혼자 공간 토핑 재편집. 화면은 그룹 재편집 화면(`CanvasToppingArrangeScreen`)을 쓴다 */
@Composable
internal fun SoloToppingArrangeRoute(
    initialToppingId: Long,
    initialIsText: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SoloToppingArrangeViewModel = hiltViewModel(
        creationCallback = { factory: SoloToppingArrangeViewModel.Factory ->
            factory.create(initialToppingId = initialToppingId, initialIsText = initialIsText)
        },
    ),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val context = LocalContext.current
    val saveFailedMessage = stringResource(R.string.solo_topping_arrange_save_failure)
    val deleteFailedMessage = stringResource(R.string.solo_topping_arrange_delete_failure)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // 목적지를 짚어 되감는다 — 이펙트가 겹쳐 와도 캔버스 밑으로 내려가지 않는다
                SoloToppingArrangeEffect.NavigateBack -> navigator.popUpTo<CanvasHomeNavKey>()

                SoloToppingArrangeEffect.SaveFailed -> toastPolicy.showError(saveFailedMessage)

                SoloToppingArrangeEffect.DeleteFailed -> toastPolicy.showError(deleteFailedMessage)

                SoloToppingArrangeEffect.TriggerSnapHaptic -> context.triggerSnapHaptic()
            }
        }
    }

    BackHandler { viewModel.processIntent(CanvasToppingArrangeIntent.OnSystemBack) }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
    ) { innerPadding ->
        CanvasToppingArrangeScreen(
            uiState = uiState,
            onClickClose = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickClose) },
            onClickConfirm = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickConfirm) },
            onClickTopping = { topping ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnClickTopping(topping))
            },
            onClickText = { text -> viewModel.processIntent(CanvasToppingArrangeIntent.OnClickText(text)) },
            onClickEmptyCanvas = { viewModel.processIntent(CanvasToppingArrangeIntent.OnClickEmptyCanvas) },
            onToggleBorderPanel = { viewModel.processIntent(CanvasToppingArrangeIntent.OnToggleBorderPanel) },
            onDismissBorderPanel = { viewModel.processIntent(CanvasToppingArrangeIntent.OnDismissBorderPanel) },
            onSelectBorderColor = { colorArgb ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnSelectBorderColor(colorArgb))
            },
            onChangeBorderWidth = { widthDp ->
                viewModel.processIntent(CanvasToppingArrangeIntent.OnChangeBorderWidth(widthDp))
            },
            onToppingTransform = { panX, panY, zoom, rotationDelta, thresholdRatioX, thresholdRatioY ->
                viewModel.processIntent(
                    CanvasToppingArrangeIntent.OnToppingTransform(
                        panX = panX,
                        panY = panY,
                        zoom = zoom,
                        rotationDelta = rotationDelta,
                        thresholdRatioX = thresholdRatioX,
                        thresholdRatioY = thresholdRatioY,
                    ),
                )
            },
            onToppingTransformGestureEnd = {
                viewModel.processIntent(CanvasToppingArrangeIntent.OnToppingTransformGestureEnd)
            },
            onClickDeleteTopping = {
                viewModel.processIntent(CanvasToppingArrangeIntent.OnClickDeleteToppingButton)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            // 그룹 재편집과 같은 이유로 스캐폴드에 넘기지 않는다 — 스캐폴드의 토스트 자리는 헤더를 덮는다
            toastPolicy = toastPolicy,
        )
    }

    if (uiState.showQuitDialog) {
        YGModalQuitEdit(
            onConfirmQuit = { viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogConfirm) },
            onDismiss = { viewModel.processIntent(CanvasToppingArrangeIntent.OnQuitDialogCancel) },
        )
    }
}
