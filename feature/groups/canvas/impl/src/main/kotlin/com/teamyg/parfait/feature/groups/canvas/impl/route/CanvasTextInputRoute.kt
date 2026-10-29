package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitAddText
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.model.PlaceFailure
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasTextInputScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputViewModel

@Composable
internal fun CanvasTextInputRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasTextInputViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val placeFailedMessage = stringResource(R.string.canvas_text_place_failed)
    val placeFailedPermanentlyMessage = stringResource(R.string.canvas_text_place_failed_permanently)
    val placeUncertainMessage = stringResource(R.string.canvas_text_place_uncertain)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasTextInputEffect.NavigateBack -> navigator.onBack()

                // 되감지 않고 알린다 — 입력한 글을 잃지 않고 다시 시도하거나 직접 닫을 수 있다
                is CanvasTextInputEffect.ShowPlaceFailed -> toastPolicy.showError(
                    when (effect.failure) {
                        PlaceFailure.Retryable -> placeFailedMessage
                        PlaceFailure.Permanent -> placeFailedPermanentlyMessage
                        PlaceFailure.Uncertain -> placeUncertainMessage
                    },
                )
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) {
        CanvasTextInputScreen(
            uiState = uiState,
            onContentChange = { value -> viewModel.processIntent(CanvasTextInputIntent.OnContentChange(value)) },
            onSelectColor = { color -> viewModel.processIntent(CanvasTextInputIntent.OnSelectColor(color)) },
            onToggleColorPopover = { viewModel.processIntent(CanvasTextInputIntent.OnToggleColorPopover) },
            onDismissColorPopover = { viewModel.processIntent(CanvasTextInputIntent.OnDismissColorPopover) },
            onAutoFontSizeChange = { designFontSize ->
                viewModel.processIntent(CanvasTextInputIntent.OnAutoFontSizeChange(designFontSize))
            },
            onClickType = { viewModel.processIntent(CanvasTextInputIntent.OnClickType) },
            onClickAlign = { viewModel.processIntent(CanvasTextInputIntent.OnClickAlign) },
            onCanvasMeasured = { size -> viewModel.processIntent(CanvasTextInputIntent.OnCanvasMeasured(size)) },
            onTextBoxMeasured = { size -> viewModel.processIntent(CanvasTextInputIntent.OnTextBoxMeasured(size)) },
            onToppingTransform = { pan, zoom, rotationDelta ->
                viewModel.processIntent(
                    CanvasTextInputIntent.OnToppingTransform(pan = pan, zoom = zoom, rotationDelta = rotationDelta),
                )
            },
            onDragPlacementSeed = { offsetX, offsetY ->
                viewModel.processIntent(CanvasTextInputIntent.OnDragPlacementSeed(offsetX = offsetX, offsetY = offsetY))
            },
            onClickConfirm = { viewModel.processIntent(CanvasTextInputIntent.OnClickConfirm) },
            onClickClose = { viewModel.processIntent(CanvasTextInputIntent.OnClickClose) },
            onClickStack = { viewModel.processIntent(CanvasTextInputIntent.OnClickStack) },
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (uiState.showQuitDialog) {
        YGModalQuitAddText(
            onConfirmQuit = { viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogConfirm) },
            onDismiss = { viewModel.processIntent(CanvasTextInputIntent.OnQuitDialogCancel) },
        )
    }
}
