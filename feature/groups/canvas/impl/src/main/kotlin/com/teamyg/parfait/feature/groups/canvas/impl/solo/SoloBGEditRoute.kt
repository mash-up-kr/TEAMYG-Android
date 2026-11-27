package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasBGEditScreen
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasBGEditIntent

/** 혼자 공간 배경 편집. 그룹 배경 편집 화면을 그대로 쓰고 저장 대상만 다르다 */
@Composable
internal fun SoloBGEditRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SoloBGEditViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val saveFailedMessage = stringResource(R.string.solo_bg_edit_save_failure)

    ResultEffect<PictureConfirmResult> { result ->
        viewModel.processIntent(
            CanvasBGEditIntent.OnBackgroundImageResult(uri = result.uri, source = result.source),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SoloBGEditEffect.NavigateToCamera -> {
                    navigator.goTo(destination = NavKeyCameraCustom(showGuideToast = false, returnResultOnly = true))
                }

                SoloBGEditEffect.NavigateToGallery -> {
                    navigator.goTo(
                        destination = NavKeyCustomGalleryPicker(
                            recentImagePick = RecentImagePick.SOURCE,
                            showGuideToast = false,
                            returnResultOnly = true,
                        ),
                    )
                }

                SoloBGEditEffect.NavigateBack, SoloBGEditEffect.Close -> navigator.onBack()

                SoloBGEditEffect.ShowSaveFailed -> toastPolicy.showError(saveFailedMessage)
            }
        }
    }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = uiState.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        CanvasBGEditScreen(
            uiState = uiState,
            onSelectColor = { color -> viewModel.processIntent(CanvasBGEditIntent.OnSelectColor(color)) },
            onClickCamera = { viewModel.processIntent(CanvasBGEditIntent.OnClickCamera) },
            onClickGallery = { viewModel.processIntent(CanvasBGEditIntent.OnClickGallery) },
            onClickCloseButton = { viewModel.processIntent(CanvasBGEditIntent.OnClickCloseButton) },
            onQuitDialogConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogConfirm) },
            onQuitDialogCancel = { viewModel.processIntent(CanvasBGEditIntent.OnQuitDialogCancel) },
            onClickConfirm = { viewModel.processIntent(CanvasBGEditIntent.OnClickConfirm) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
