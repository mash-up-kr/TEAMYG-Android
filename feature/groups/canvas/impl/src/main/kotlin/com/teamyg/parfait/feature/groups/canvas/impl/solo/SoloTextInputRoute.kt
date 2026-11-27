package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitAddText
import com.teamyg.parfait.core.util.android.extension.triggerSnapHaptic
import com.teamyg.parfait.feature.groups.canvas.api.CanvasHomeNavKey
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasTextInputScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasTextInputIntent

/** 혼자 공간 텍스트 입력. 화면은 그룹 텍스트 입력 화면(`CanvasTextInputScreen`)을 쓴다 */
@Composable
internal fun SoloTextInputRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SoloTextInputViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val context = LocalContext.current
    val placeFailedMessage = stringResource(R.string.solo_text_input_failure)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SoloTextInputEffect.NavigateBack -> navigator.popUpTo<CanvasHomeNavKey>()

                // 되감지 않고 알린다 — 입력한 글을 잃지 않고 다시 시도하거나 직접 닫을 수 있다
                SoloTextInputEffect.PlaceFailed -> toastPolicy.showError(placeFailedMessage)

                SoloTextInputEffect.TriggerSnapHaptic -> context.triggerSnapHaptic()
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
            onToppingTransformGestureEnd = {
                viewModel.processIntent(CanvasTextInputIntent.OnToppingTransformGestureEnd)
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
