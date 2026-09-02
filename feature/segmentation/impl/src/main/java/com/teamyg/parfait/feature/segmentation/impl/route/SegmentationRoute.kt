package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.foundation.layout.padding
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
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationErrorScreen
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationViewModel

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

                // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
                is SegmentationEffect.GoToConfirm -> navigator.goTo(
                    NavKeySegmentationConfirm(
                        sourceImageUri = key.sourceImageUri,
                        subjectImagePath = effect.subjectImagePath,
                        trimmedSubjectImagePath = effect.trimmedSubjectImagePath,
                    ),
                )
            }
        }
    }

    // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
    val onClickClose: () -> Unit = { navigator.popUpTo<NavKeyCanvasMain>() }

    YGScaffoldV2(
        isLoading = state.isLoading,
        toastPolicy = toastPolicy,
    ) { innerPadding ->
        // 대상을 아예 못 얻은 실패는 화면 전체를 C-103-Error 로 바꾼다.
        // 고른 뒤의 실패는 후보가 남아 있어 토스트로만 알린다(SegmentationEffect.ShowError)
        if (state.isError) {
            SegmentationErrorScreen(
                onClickClose = onClickClose,
                modifier = modifier.padding(innerPadding),
            )
        } else {
            SegmentationScreen(
                state = state,
                modifier = modifier.padding(innerPadding),
                onClickBack = { navigator.onBack() },
                onClickClose = onClickClose,
                onClickCandidate = { index ->
                    viewModel.processIntent(SegmentationIntent.ClickCandidate(index))
                },
            )
        }
    }
}
