package com.teamyg.parfait.feature.segmentation.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationConfirm
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationScreen
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

    YGScaffoldV2 { innerPadding ->
        SegmentationScreen(
            state = state,
            modifier = modifier.padding(innerPadding),
            onClickBack = { navigator.onBack() },
            // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
            onClickClose = { navigator.popUpTo<NavKeyCanvasMain>() },
            // 백스택에 쌓아 올려서 뒤로가기 하면 객체 인식이 끝난 이 화면으로 그대로 돌아온다
            onClickSubject = {
                val subjectImagePath = state.subjectImagePath
                val trimmedSubjectImagePath = state.trimmedSubjectImagePath
                if (subjectImagePath != null && trimmedSubjectImagePath != null) {
                    navigator.goTo(
                        NavKeySegmentationConfirm(
                            sourceImageUri = key.sourceImageUri,
                            subjectImagePath = subjectImagePath,
                            trimmedSubjectImagePath = trimmedSubjectImagePath,
                        ),
                    )
                }
            },
        )
    }
}
