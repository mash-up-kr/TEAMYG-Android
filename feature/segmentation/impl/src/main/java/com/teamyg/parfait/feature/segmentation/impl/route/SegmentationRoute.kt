package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationErrorScreen
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

    ResultEffect<ToppingEditResult>(resultKey = TOPPING_EDIT_RESULT_KEY) { result ->
        viewModel.processIntent(SegmentationIntent.OnEditResult(result))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SegmentationEffect.ShowError -> toastPolicy.showError(errorMessage)

                is SegmentationEffect.GoBack -> navigator.onBack()

                is SegmentationEffect.GoToEdit -> navigator.goTo(
                    NavKeyToppingEdit(
                        sourceImageUri = key.sourceImageUri,
                        segmentationImageUri = File(effect.originImagePath).toUri().toString(),
                    ),
                )

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
        if (state.isError) {
            SegmentationErrorScreen(
                onClickRetry = { viewModel.processIntent(SegmentationIntent.Retry) },
                onClickEditManually = { viewModel.processIntent(SegmentationIntent.EditManually) },
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
