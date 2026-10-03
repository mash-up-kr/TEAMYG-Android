package com.teamyg.parfait.feature.segmentation.impl.route

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.modal.YGModalQuitEdit
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
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
                isDetectionFailed = key.isDetectionFailed,
            )
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toastPolicy = rememberYGToastPolicy()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ToppingEditEffect.LoadFailed -> {
                    // 바로 화면을 닫아 화면 내 호스트로는 보이지 않는다
                    val message = context.getString(R.string.topping_edit_load_failed)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    navigator.onBack()
                }

                is ToppingEditEffect.SaveFailed -> {
                    toastPolicy.showError(context.getString(R.string.topping_edit_save_failed))
                }

                is ToppingEditEffect.SubjectTooSmall -> {
                    toastPolicy.showError(context.getString(R.string.topping_edit_subject_too_small))
                }

                is ToppingEditEffect.DraftUnavailable -> {
                    toastPolicy.showError(context.getString(R.string.topping_edit_draft_unavailable))
                }

                is ToppingEditEffect.GoToPlace -> {
                    // 저장 중 표시가 이동 전에 내려가 그 틈의 탭이 한 번 더 온다. 맨 위일 때만 이동한다
                    if (navigator.backStack.lastOrNull() == key) {
                        navigator.goTo(NavKeyCanvasToppingPlace)
                    }
                }

                is ToppingEditEffect.QuitToCanvas -> navigator.popUpTo<NavKeyCanvasMain>()

                is ToppingEditEffect.ShowDetectionFailed -> {
                    toastPolicy.show(
                        YGToastType.Edit(context.getString(R.string.topping_edit_detection_failed)),
                    )
                }
            }
        }
    }

    YGScaffoldV2(isLoading = state.isLoading || state.isSaving) { innerPadding ->
        ToppingEditScreen(
            state = state,
            onChangeMode = { mode -> viewModel.processIntent(ToppingEditIntent.ChangeMode(mode)) },
            onChangeBrushWidth = { width -> viewModel.processIntent(ToppingEditIntent.ChangeBrushWidth(width)) },
            onAddStroke = { stroke -> viewModel.processIntent(ToppingEditIntent.AddStroke(stroke)) },
            onClickUndoArea = { viewModel.processIntent(ToppingEditIntent.UndoArea) },
            onClickRedoArea = { viewModel.processIntent(ToppingEditIntent.RedoArea) },
            onClickNext = { viewModel.processIntent(ToppingEditIntent.ClickDone) },
            onClickBack = navigator::onBack,
            onClickClose = { viewModel.processIntent(ToppingEditIntent.ClickClose) },
            toast = { YGToastHost(policy = toastPolicy, modifier = Modifier.fillMaxWidth()) },
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
