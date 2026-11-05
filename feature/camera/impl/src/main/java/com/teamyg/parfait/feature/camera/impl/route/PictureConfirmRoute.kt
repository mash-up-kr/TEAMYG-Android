package com.teamyg.parfait.feature.camera.impl.route

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitAdd
import com.teamyg.parfait.core.ui.component.modal.YGModalQuitBackground
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.PictureConfirmResult
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.screen.PictureConfirmScreen
import com.teamyg.parfait.feature.camera.impl.viewmodel.PictureConfirmEffect
import com.teamyg.parfait.feature.camera.impl.viewmodel.PictureConfirmIntent
import com.teamyg.parfait.feature.camera.impl.viewmodel.PictureConfirmViewModel
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.segmentation.api.NavKeySegmentation

@Composable
internal fun PictureConfirmRoute(
    uri: String,
    source: PictureConfirmSource,
    returnResultOnly: Boolean,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: PictureConfirmViewModel = hiltViewModel<PictureConfirmViewModel, PictureConfirmViewModel.Factory>(
        creationCallback = { it.create(uri, source, returnResultOnly) },
    ),
) {
    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                PictureConfirmEffect.NavigateBack -> navigator.onBack()

                is PictureConfirmEffect.ReturnResult -> {
                    resultEventBus.sendResult(PictureConfirmResult(uri = effect.uri, source = effect.source))
                    // 사이에 낀 촬영·선택 화면을 몇 장이든 걷고 부른 화면으로 되감는다.
                    // 뒤로가기를 세어 돌아가면 그 경로에 화면이 하나 끼는 날 조용히 어긋난다
                    navigator.popUpTo<NavKeyCanvasBGEdit>()
                }

                is PictureConfirmEffect.NavigateToSegmentation ->
                    // 세그멘테이션 로딩이 이 화면 위에서 돈다. 실패·뒤로가기 때 돌아올 수 있게 백스택에 남기고,
                    // 연타로 같은 키가 겹쳐 쌓이지 않게 한다
                    navigator.goToSingleClearTop(
                        destination = NavKeySegmentation(sourceImageUri = effect.uri),
                    )

                PictureConfirmEffect.QuitToCanvas -> navigator.popUpTo<NavKeyCanvasMain>()
            }
        }
    }

    YGScaffoldV2 { innerPadding ->
        PictureConfirmScreen(
            uri = uri,
            source = source,
            onClickRetake = { viewModel.processIntent(PictureConfirmIntent.OnClickRetake) },
            onClickConfirm = { viewModel.processIntent(PictureConfirmIntent.OnClickConfirm) },
            onClickClose = { viewModel.processIntent(PictureConfirmIntent.OnClickClose) },
            modifier = modifier.padding(innerPadding),
        )
    }

    if (state.showQuitDialog) {
        val onConfirmQuit = { viewModel.processIntent(PictureConfirmIntent.OnConfirmQuit) }
        val onDismiss = { viewModel.processIntent(PictureConfirmIntent.OnDismissQuit) }
        if (returnResultOnly) {
            YGModalQuitBackground(onConfirmQuit = onConfirmQuit, onDismiss = onDismiss)
        } else {
            YGModalQuitAdd(onConfirmQuit = onConfirmQuit, onDismiss = onDismiss)
        }
    }
}
