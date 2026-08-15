package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

@Composable
internal fun CanvasBGEditRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasBGEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    ResultEffect<PictureConfirmResult> { result ->
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(uri = result.uri, source = result.source),
        )
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
        modifier = modifier,
    )
}
