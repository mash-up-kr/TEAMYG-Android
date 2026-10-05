package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitEdit
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationLoadingScreen
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationViewModel
import java.io.File

@Composable
internal fun SegmentationRoute(
    navigator: Navigator,
    key: NavKeySegmentation,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationViewModel, SegmentationViewModel.Factory>(
        creationCallback = { factory -> factory.create(key.sourceImageUri) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastPolicy = rememberYGToastPolicy()
    val errorMessage = stringResource(R.string.segmentation_error_message)

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SegmentationEffect.ShowError -> toastPolicy.showError(errorMessage)

                // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
                is SegmentationEffect.QuitToCanvas -> navigator.popUpTo<NavKeyCanvasMain>()

                // 이 화면을 걷어 편집에서 뒤로가기 해도 분석으로 돌아오지 않게 한다
                is SegmentationEffect.GoToEditDetectionFailed -> navigator.goToAndPopCurrent(
                    NavKeyToppingEdit(
                        sourceImageUri = key.sourceImageUri,
                        segmentationImageUri = key.sourceImageUri,
                        isDetectionFailed = true,
                    ),
                )

                // 쌓아서 뒤로가기 하면 선택 UI 로 돌아오게 한다. 이펙트가 닿은 뒤 이동하기 전에
                // 들어온 탭이 편집을 두 번 쌓지 않게 맨 위일 때만 간다
                is SegmentationEffect.GoToEditCandidate -> if (navigator.backStack.lastOrNull() == key) {
                    navigator.goTo(
                        NavKeyToppingEdit(
                            sourceImageUri = key.sourceImageUri,
                            segmentationImageUri = File(effect.segmentationImagePath).toUri().toString(),
                        ),
                    )
                }
            }
        }
    }

    // 분석 중에는 되돌아갈 화면이 없다 — 뒤로가기도 X 와 같이 그만두기를 묻는다
    BackHandler(enabled = state.isAnalyzing) {
        viewModel.processIntent(SegmentationIntent.ClickClose)
    }

    YGScaffoldV2(
        isLoading = state.isSaving,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        if (state.isAnalyzing) {
            SegmentationLoadingScreen(
                onClickClose = { viewModel.processIntent(SegmentationIntent.ClickClose) },
                modifier = modifier.padding(innerPadding),
            )
        } else {
            SegmentationScreen(
                state = state,
                modifier = modifier.padding(innerPadding),
                onClickBack = { navigator.onBack() },
                onClickClose = { viewModel.processIntent(SegmentationIntent.ClickClose) },
                onClickCandidate = { index ->
                    viewModel.processIntent(SegmentationIntent.ClickCandidate(index))
                },
            )
        }
    }

    if (state.showQuitDialog) {
        YGModalQuitEdit(
            onConfirmQuit = { viewModel.processIntent(SegmentationIntent.ConfirmQuit) },
            onDismiss = { viewModel.processIntent(SegmentationIntent.DismissQuit) },
        )
    }
}
