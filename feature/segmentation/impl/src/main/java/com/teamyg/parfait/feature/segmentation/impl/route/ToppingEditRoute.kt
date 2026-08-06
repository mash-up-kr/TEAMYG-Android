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
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.impl.screen.ToppingEditScreen
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditEffect
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditIntent
import com.teamyg.parfait.feature.segmentation.impl.viewmodel.ToppingEditViewModel

@Composable
internal fun ToppingEditRoute(
    navigator: Navigator,
    key: NavKeyToppingEdit,
    modifier: Modifier = Modifier,
) {
    val viewModel = hiltViewModel<ToppingEditViewModel, ToppingEditViewModel.Factory>(
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
                is ToppingEditEffect.LoadFailed -> {
                    Toast.makeText(context, "이미지를 불러오지 못했습니다", Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }

                // Todo : core:ui 에 string resource 로 분리
                is ToppingEditEffect.SaveFailed -> {
                    Toast.makeText(context, "편집 결과를 저장하지 못했습니다", Toast.LENGTH_SHORT).show()
                }

                // 확인 화면이 편집 전 이미지를 들고 있으므로, 편집본 경로를 결과로 넘기고 돌아간다
                is ToppingEditEffect.EditCompleted -> {
                    resultEventBus.sendResult(TOPPING_EDIT_RESULT_KEY, effect.editedImagePath)
                    navigator.onBack()
                }
            }
        }
    }

    ToppingEditScreen(
        state = state,
        onChangeTab = { tab -> viewModel.processIntent(ToppingEditIntent.ChangeTab(tab)) },
        onChangeMode = { mode -> viewModel.processIntent(ToppingEditIntent.ChangeMode(mode)) },
        onChangeBrushWidth = { width -> viewModel.processIntent(ToppingEditIntent.ChangeBrushWidth(width)) },
        onAddStroke = { stroke -> viewModel.processIntent(ToppingEditIntent.AddStroke(stroke)) },
        onClickUndo = { viewModel.processIntent(ToppingEditIntent.Undo) },
        onClickRedo = { viewModel.processIntent(ToppingEditIntent.Redo) },
        onClickDone = { viewModel.processIntent(ToppingEditIntent.ClickDone) },
        onClickBack = { navigator.onBack() },
        modifier = modifier,
    )
}
