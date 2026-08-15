package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasBGEditScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditViewModel
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult

@Composable
internal fun CanvasBGEditRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasBGEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    // NavKey는 결과를 안 담아오므로, 편집을 보낸 토핑 id를 기억해뒀다가 결과가 오면 그 토핑에 반영한다
    var editingToppingId by rememberSaveable { mutableStateOf<Long?>(null) }

    ResultEffect<PictureConfirmResult> { result ->
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(uri = result.uri, source = result.source),
        )
    }

    ResultEffect<ToppingEditResult>(resultKey = TOPPING_EDIT_RESULT_KEY) { result ->
        val toppingId = editingToppingId ?: return@ResultEffect
        viewModel.processIntent(CanvasBGEditIntent.OnToppingEditResult(toppingId, result))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasBGEditEffect.NavigateToCamera -> {
                    navigator.goTo(destination = NavKeyCameraCustom(showGuideToast = false, returnResultOnly = true))
                }

                is CanvasBGEditEffect.NavigateToGallery -> {
                    navigator.goTo(
                        destination = NavKeyCustomGalleryPicker(showGuideToast = false, returnResultOnly = true),
                    )
                }

                is CanvasBGEditEffect.NavigateBack -> navigator.onBack()

                is CanvasBGEditEffect.ConfirmBackground -> {
                    // TODO: 선택한 배경을 서버에 업로드/저장하는 연동 필요 - effect.background 사용
                    navigator.onBack()
                }

                is CanvasBGEditEffect.NavigateToToppingEdit -> {
                    editingToppingId = effect.toppingId
                    navigator.goTo(
                        destination = NavKeyToppingEdit(
                            sourceImageUri = effect.sourceImageUri,
                            segmentationImageUri = effect.segmentationImageUri,
                            borderLayers = effect.borderLayers,
                            // 캔버스에 이미 놓인 토핑을 다시 손보는 거라 영역(잘라내기)은 다시 건드릴 수 없다
                            borderOnly = true,
                        ),
                    )
                }
            }
        }
    }

    CanvasBGEditScreen(
        uiState = uiState,
        onSelectTab = { tab -> viewModel.processIntent(CanvasBGEditIntent.OnSelectTab(tab)) },
        onSelectColor = { color -> viewModel.processIntent(CanvasBGEditIntent.OnSelectColor(color)) },
        onClickCamera = { viewModel.processIntent(CanvasBGEditIntent.OnClickCamera()) },
        onClickGallery = { viewModel.processIntent(CanvasBGEditIntent.OnClickGallery()) },
        onClickCloseButton = { viewModel.processIntent(CanvasBGEditIntent.OnClickCloseButton()) },
        onQuitDialogConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogConfirm()) },
        onQuitDialogCancel = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogCancel()) },
        onClickConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm()) },
        onClickTopping = { topping -> viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(topping)) },
        onClickDeselectTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickDeselectTopping()) },
        onClickDeleteTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickDeleteToppingButton()) },
        onDeleteToppingDialogConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm()) },
        onDeleteToppingDialogCancel = { viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogCancel()) },
        onClickEditTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickEditTopping()) },
        onToppingResizeDrag = { delta -> viewModel.processIntent(CanvasBGEditIntent.OnToppingResizeDrag(delta)) },
        onToppingRotateDrag = { delta -> viewModel.processIntent(CanvasBGEditIntent.OnToppingRotateDrag(delta)) },
        onToppingMoveDrag = { delta -> viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(delta)) },
        modifier = modifier,
    )
}
