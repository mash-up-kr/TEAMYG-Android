package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentationEdit
import com.teamyg.parfait.feature.segmentation.impl.screen.SegmentationEditScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEditEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEditIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.SegmentationEditViewModel

@Composable
internal fun SegmentationEditRoute(
    navigator: Navigator,
    key: NavKeySegmentationEdit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<SegmentationEditViewModel, SegmentationEditViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(
                sourceImageUri = key.sourceImageUri,
                segmentationImageUri = key.segmentationImageUri,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resultEventBus = LocalResultEventBus.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SegmentationEditEffect.LoadFailed -> {
                    Toast.makeText(context, "이미지를 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }

                // Todo : core:ui 에 string resource 로 분리
                is SegmentationEditEffect.SaveFailed -> {
                    Toast.makeText(context, "편집 결과를 저장하지 못했습니다", Toast.LENGTH_SHORT).show()
                }

                // 확인 화면이 편집 전 이미지를 들고 있으므로, 편집본 경로를 결과로 넘기고 돌아간다
                is SegmentationEditEffect.EditCompleted -> {
                    resultEventBus.sendResult(effect.editedImagePath)
                    navigator.onBack()
                }
            }
        }
    }

    SegmentationEditScreen(
        state = state,
        onChangeTab = { tab -> viewModel.processIntent(SegmentationEditIntent.ChangeTab(tab)) },
        onChangeMode = { mode -> viewModel.processIntent(SegmentationEditIntent.ChangeMode(mode)) },
        onChangeBrushWidth = { width -> viewModel.processIntent(SegmentationEditIntent.ChangeBrushWidth(width)) },
        onAddStroke = { stroke -> viewModel.processIntent(SegmentationEditIntent.AddStroke(stroke)) },
        onClickUndo = { viewModel.processIntent(SegmentationEditIntent.Undo) },
        onClickRedo = { viewModel.processIntent(SegmentationEditIntent.Redo) },
        onClickReset = { viewModel.processIntent(SegmentationEditIntent.Reset) },
        onClickDone = { viewModel.processIntent(SegmentationEditIntent.ClickDone) },
        onClickBack = { navigator.onBack() },
        modifier = modifier,
    )
}
