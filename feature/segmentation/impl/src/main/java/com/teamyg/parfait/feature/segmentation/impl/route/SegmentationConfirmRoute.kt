package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.api.TOPPING_EDIT_RESULT_KEY
import com.teamyg.parfait.feature.segmentation.api.ToppingEditResult
import com.teamyg.parfait.feature.segmentation.impl.R
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationConfirmScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationConfirmEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationConfirmIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationConfirmViewModel
import java.io.File

@Composable
internal fun SegmentationConfirmRoute(
    navigator: Navigator,
    key: NavKeySegmentationConfirm,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationConfirmViewModel, SegmentationConfirmViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                // 미리보기·배치에 넘길 값이라 투명 여백을 걷어낸 판으로 연다.
                // 재편집 마스크는 원본 좌표계를 지켜야 해 걷지 않은 판이다
                subjectImagePath = key.trimmedSubjectImagePath,
                cutoutImagePath = key.subjectImagePath,
            )
        },
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()

    ResultEffect<ToppingEditResult>(resultKey = TOPPING_EDIT_RESULT_KEY) { result ->
        viewModel.processIntent(SegmentationConfirmIntent.OnEditResult(result))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            val message = when (effect) {
                SegmentationConfirmEffect.DraftMissing,
                SegmentationConfirmEffect.DraftWriteFailed,
                -> context.getString(R.string.segmentation_confirm_draft_unavailable)
            }
            toastPolicy.showError(message)
        }
    }

    YGScaffoldV2(toastPolicy = toastPolicy) { innerPadding ->
        SegmentationConfirmScreen(
            subjectImagePath = uiState.subjectImagePath,
            borderColorArgb = uiState.borderColorArgb,
            borderWidthDp = uiState.borderWidthDp,
            isNextEnabled = uiState.isDraftReady,
            onClickBack = { navigator.onBack() },
            // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
            onClickClose = { navigator.popUpTo<NavKeyCanvasMain>() },
            onClickEditPhoto = {
                navigator.goTo(
                    NavKeyToppingEdit(
                        sourceImageUri = key.sourceImageUri,
                        // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                        segmentationImageUri = File(uiState.cutoutImagePath).toUri().toString(),
                        borderLayers = uiState.borderLayers,
                    ),
                )
            },
            onClickNext = {
                navigator.goTo(NavKeyCanvasToppingPlace(imageUri = File(uiState.subjectImagePath).toUri().toString()))
            },
            modifier = modifier.padding(innerPadding),
        )
    }
}
