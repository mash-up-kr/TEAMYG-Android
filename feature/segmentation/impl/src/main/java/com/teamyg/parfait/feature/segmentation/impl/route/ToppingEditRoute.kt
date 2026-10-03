package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.modal.YGModalQuitEdit
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasToppingPlace
import com.teamyg.parfait.feature.segmentation.api.NavKeyToppingEdit
import com.teamyg.parfait.feature.segmentation.impl.R
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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ToppingEditEffect.LoadFailed -> {
                    val message = context.getString(R.string.topping_edit_load_failed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }

                is ToppingEditEffect.SaveFailed -> {
                    val message = context.getString(R.string.topping_edit_save_failed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is ToppingEditEffect.SubjectTooSmall -> {
                    val message = context.getString(R.string.topping_edit_subject_too_small)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is ToppingEditEffect.DraftUnavailable -> {
                    val message = context.getString(R.string.topping_edit_draft_unavailable)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is ToppingEditEffect.GoToPlace -> {
                    // 저장 중 표시는 이동 전에 내려가므로, 이 화면이 걷히기 전에 들어온 완료 탭은 한 번 더
                    // 끝까지 간다. 이 화면이 맨 위일 때만 이동해 토핑 배치(C-106)가 두 번 쌓이지 않게 한다
                    if (navigator.backStack.lastOrNull() == key) {
                        navigator.goTo(NavKeyCanvasToppingPlace)
                    }
                }

                // 토핑 만들기를 접고 캔버스로 돌아간다. 사이에 쌓인 화면은 모두 걷는다
                is ToppingEditEffect.QuitToCanvas -> navigator.popUpTo<NavKeyCanvasMain>()
            }
        }
    }

    YGScaffoldV2 { innerPadding ->
        ToppingEditScreen(
            state = state,
            onChangeMode = { mode -> viewModel.processIntent(ToppingEditIntent.ChangeMode(mode)) },
            onChangeBrushWidth = { width -> viewModel.processIntent(ToppingEditIntent.ChangeBrushWidth(width)) },
            onAddStroke = { stroke -> viewModel.processIntent(ToppingEditIntent.AddStroke(stroke)) },
            onClickUndoArea = { viewModel.processIntent(ToppingEditIntent.UndoArea) },
            onClickRedoArea = { viewModel.processIntent(ToppingEditIntent.RedoArea) },
            onClickDone = { viewModel.processIntent(ToppingEditIntent.ClickDone) },
            onClickBack = { viewModel.processIntent(ToppingEditIntent.ClickClose) },
            modifier = modifier.padding(innerPadding),
        )
    }

    if (state.showQuitDialog) {
        YGModalQuitEdit(
            onConfirmQuit = { viewModel.processIntent(ToppingEditIntent.ConfirmQuit) },
            onDismiss = { viewModel.processIntent(ToppingEditIntent.DismissQuit) },
        )
    }
}
