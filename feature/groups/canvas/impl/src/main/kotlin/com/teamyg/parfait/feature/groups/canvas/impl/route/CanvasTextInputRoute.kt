package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CanvasTextInputEffect.NavigateBack -> navigator.onBack()
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        CanvasTextInputScreen(
            uiState = uiState,
            onContentChange = { value -> viewModel.processIntent(CanvasTextInputIntent.OnContentChange(value)) },
            onSelectColor = { color -> viewModel.processIntent(CanvasTextInputIntent.OnSelectColor(color)) },
            onToggleColorPopover = { viewModel.processIntent(CanvasTextInputIntent.OnToggleColorPopover) },
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
}
