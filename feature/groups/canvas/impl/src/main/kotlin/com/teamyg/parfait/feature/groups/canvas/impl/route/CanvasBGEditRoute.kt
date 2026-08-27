package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasBGEditScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditError
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditViewModel
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.toStringResource
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult

@Composable
internal fun CanvasBGEditRoute(
    groupId: Long,
    parfaitId: Long,
    navigator: Navigator,
    initialToppingId: Long? = null,
    modifier: Modifier = Modifier,
    viewModel: CanvasBGEditViewModel = hiltViewModel(
        creationCallback = { factory: CanvasBGEditViewModel.Factory ->
            factory.create(
                groupIdValue = groupId,
                parfaitIdValue = parfaitId,
                initialToppingIdValue = initialToppingId,
            )
        },
    ),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()

    var editingToppingId by rememberSaveable { mutableStateOf<Long?>(null) }

    // 이펙트 수집은 컴포지션이 아니라 코루틴이라 그 안에서 `stringResource` 를 부를 수 없다.
    // 문구를 여기서 미리 뽑아 두고 이펙트는 고르기만 한다
    val errorMessages = CanvasBGEditError.entries.associateWith { it.toStringResource() }

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

                // 실린 배경을 쓰지 않는 이유: 돌아간 캔버스 메인이 다시 조회해 그린다
                is CanvasBGEditEffect.ConfirmBackground -> navigator.onBack()

                is CanvasBGEditEffect.ShowError -> toastPolicy.showError(errorMessages.getValue(effect.error))

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

    YGScaffoldV2(
        modifier = modifier,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        CanvasBGEditScreen(
            uiState = uiState,
            onSelectTab = { tab -> viewModel.processIntent(CanvasBGEditIntent.OnSelectTab(tab)) },
            onSelectColor = { color -> viewModel.processIntent(CanvasBGEditIntent.OnSelectColor(color)) },
            onClickCamera = { viewModel.processIntent(CanvasBGEditIntent.OnClickCamera) },
            onClickGallery = { viewModel.processIntent(CanvasBGEditIntent.OnClickGallery) },
            onClickCloseButton = { viewModel.processIntent(CanvasBGEditIntent.OnClickCloseButton) },
            onQuitDialogConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogConfirm) },
            onQuitDialogCancel = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogCancel) },
            onClickConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm) },
            onClickTopping = { topping -> viewModel.processIntent(CanvasBGEditIntent.OnClickTopping(topping)) },
            onClickDeselectTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickDeselectTopping) },
            onClickDeleteTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickDeleteToppingButton) },
            onDeleteToppingDialogConfirm = {
                viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogConfirm)
            },
            onDeleteToppingDialogCancel = {
                viewModel.processIntent(CanvasBGEditIntent.OnDeleteToppingDialogCancel)
            },
            onClickEditTopping = { viewModel.processIntent(CanvasBGEditIntent.OnClickEditTopping) },
            onToppingResizeDrag = { delta -> viewModel.processIntent(CanvasBGEditIntent.OnToppingResizeDrag(delta)) },
            onToppingRotateDrag = { delta -> viewModel.processIntent(CanvasBGEditIntent.OnToppingRotateDrag(delta)) },
            onToppingMoveDrag = { deltaX, deltaY ->
                viewModel.processIntent(CanvasBGEditIntent.OnToppingMoveDrag(deltaX = deltaX, deltaY = deltaY))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
