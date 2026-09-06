package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.component.ygtutorial.YGTutorialOverlay
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
                // 두 인자의 이름이 서로 반대 의미라 뒤바꾸기 쉽다(`ToppingEditResult` KDoc)
                subjectImagePath = key.trimmedSubjectImagePath,
                cutoutImagePath = key.subjectImagePath,
                sourceImageUri = key.sourceImageUri,
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

    val sourceImageUri = uiState.sourceImageUri

    // 튜토리얼은 스캐폴드 **밖**에 겹친다 — 안에 넣으면 컨텐츠 인셋을 받아 딤이 상태바
    // 밑에서 끊기고, 시스템바만 안 덮인 화면이 된다
    Box(modifier = Modifier.fillMaxSize()) {
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
                    // 편집 화면은 ContentResolver 로 읽으므로 파일 경로를 file 스킴 uri 로 바꿔서 넘긴다
                    val editImageUri = File(uiState.editImagePath).toUri().toString()

                    navigator.goTo(
                        NavKeyToppingEdit(
                            // 되살릴 원본이 없는 진입은 원본 자리에도 알맹이를 넣는다 — 원본과
                            // 누끼가 같은 그림이면 편집 결과가 알맹이 그대로다
                            sourceImageUri = sourceImageUri ?: editImageUri,
                            segmentationImageUri = editImageUri,
                            borderLayers = uiState.borderLayers,
                            borderOnly = uiState.isBorderOnlyEdit,
                        ),
                    )
                },
                onClickNext = { navigator.goTo(NavKeyCanvasToppingPlace) },
                modifier = modifier.padding(innerPadding),
            )
        }

        if (uiState.isTutorialVisible) {
            // 강조 대상이 화면 아래쪽(사진 편집 버튼)이라 카드는 위에 붙인다 — 기본 배치가 그것이다
            YGTutorialOverlay(
                imageResource = R.drawable.img_segmentation_tutorial,
                title = stringResource(R.string.segmentation_confirm_tutorial_title),
                description = stringResource(R.string.segmentation_confirm_tutorial_description),
                onClickButton = { viewModel.processIntent(SegmentationConfirmIntent.OnConfirmTutorial) },
            )
        }
    }
}
