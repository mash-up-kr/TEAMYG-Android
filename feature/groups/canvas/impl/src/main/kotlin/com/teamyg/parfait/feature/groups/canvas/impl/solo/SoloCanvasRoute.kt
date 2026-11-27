package com.teamyg.parfait.feature.groups.canvas.impl.solo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.modal.YGModalPopup
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveType
import com.teamyg.parfait.feature.groups.canvas.api.NavKeySoloBGEdit
import com.teamyg.parfait.feature.groups.canvas.api.NavKeySoloTextInput
import com.teamyg.parfait.feature.groups.canvas.api.NavKeySoloToppingArrange
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadErrorOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadingOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.SoloSpaceMenu
import com.teamyg.parfait.feature.groups.canvas.impl.model.CanvasVideoSource
import com.teamyg.parfait.feature.groups.canvas.impl.model.PendingGallerySave
import com.teamyg.parfait.feature.groups.canvas.impl.model.SoloDialog
import com.teamyg.parfait.feature.groups.canvas.impl.route.CanvasSaveResultEffect
import com.teamyg.parfait.feature.groups.canvas.impl.route.openCanvasSavePreview
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.screen.toYGCanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import kotlinx.datetime.number
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 혼자 공간 캔버스. 그룹 캔버스 화면(`CanvasMainScreen`)을 그대로 쓰고, 그룹에만 있는 것(달력·멤버 칩·
 * 지난 캔버스 알림·환영 배너·그룹 설정)은 넘기지 않는다. 햄버거는 그룹 설정 대신 비우기·삭제하기 메뉴를 연다.
 */
@Composable
internal fun SoloCanvasRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(R.string.solo_canvas_title)
    val viewModel: SoloCanvasViewModel = hiltViewModel(
        creationCallback = { factory: SoloCanvasViewModel.Factory -> factory.create(title = title) },
    )
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val canvasState = uiState.canvas
    val context = LocalContext.current
    val graphicsLayer = rememberGraphicsLayer()

    // 토스트는 전부 캔버스 프레임 상단 한 큐에 띄운다 — 실패만 따로 보내면 같은 화면에서 자리가 갈리고
    // Toast 공통 정책의 스택(나중 것이 위로)도 성립하지 않는다
    val toastPolicy = rememberYGToastPolicy()
    val gallerySaveSuccessFormat = stringResource(R.string.canvas_main_gallery_save_success)
    val gallerySaveFailureMessage = stringResource(R.string.canvas_main_gallery_save_failure)
    val captureFailureMessage = stringResource(R.string.canvas_main_capture_failure)
    val canvasLoadErrorMessage = stringResource(R.string.canvas_main_today_canvas_error)
    val toppingFlowStartErrorMessage = stringResource(R.string.canvas_main_topping_flow_start_error)
    val resetFailureMessage = stringResource(R.string.solo_canvas_reset_failure)
    val deleteFailureMessage = stringResource(R.string.solo_canvas_delete_failure)

    var pendingSaveType by remember { mutableStateOf(CanvasSaveType.IMAGE) }
    var canvasAreaWidth by remember { mutableStateOf(0.dp) }
    var canvasAreaHeight by remember { mutableStateOf(0.dp) }
    var retryKey by remember { mutableIntStateOf(0) }

    CanvasSaveResultEffect(
        toastPolicy = toastPolicy,
        onSave = { pending -> viewModel.processIntent(pending.toSaveIntent()) },
        onDiscardVideo = { filePath -> viewModel.processIntent(SoloCanvasIntent.DiscardRecordedVideo(filePath)) },
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SoloCanvasEffect.NavigateToCamera -> navigator.goTo(destination = NavKeyCameraCustom())

                SoloCanvasEffect.NavigateToGallery -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker(recentImagePick = RecentImagePick.CUTOUT),
                )

                SoloCanvasEffect.NavigateToTextInput -> navigator.goTo(destination = NavKeySoloTextInput)

                SoloCanvasEffect.RequestCanvasCaptureForPreview -> graphicsLayer.openCanvasSavePreview(
                    context = context,
                    navigator = navigator,
                    saveType = pendingSaveType,
                    date = { viewModel.state.value.canvas.today },
                    videoSource = {
                        val capturedState = viewModel.state.value.canvas
                        CanvasVideoSource(
                            toppings = capturedState.toppings,
                            texts = capturedState.texts,
                            background = capturedState.canvasBackground.toYGCanvasBackground(),
                            canvasWidth = canvasAreaWidth,
                            canvasHeight = canvasAreaHeight,
                            retryKey = retryKey,
                        )
                    },
                    onFailure = { toastPolicy.showError(captureFailureMessage) },
                )

                is SoloCanvasEffect.ShowGallerySaveResult -> if (effect.isSuccess) {
                    val message = gallerySaveSuccessFormat.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(gallerySaveFailureMessage)
                }

                SoloCanvasEffect.ShowCanvasLoadError -> toastPolicy.showError(canvasLoadErrorMessage)

                SoloCanvasEffect.ShowToppingFlowStartError -> toastPolicy.showError(toppingFlowStartErrorMessage)

                SoloCanvasEffect.ShowResetFailed -> toastPolicy.showError(resetFailureMessage)

                SoloCanvasEffect.ShowDeleteFailed -> toastPolicy.showError(deleteFailureMessage)

                SoloCanvasEffect.Close -> navigator.onBack()
            }
        }
    }

    // 캔버스가 하나뿐이라 그룹 화면처럼 캔버스별로 관측을 나누지 않는다. 로딩을 한 번 본 뒤 다 그려지면
    // 그 뒤의 이미지 로딩(새 토핑)으로는 덮개를 다시 띄우지 않는다
    var loadState by remember { mutableStateOf(CanvasLoadState.Loaded) }
    var sawLoading by remember { mutableStateOf(false) }
    var firstPaintDone by remember { mutableStateOf(false) }

    LaunchedEffect(canvasState.isInitialLoading, canvasState.todayCanvas == null, loadState) {
        when {
            canvasState.isInitialLoading || loadState != CanvasLoadState.Loaded -> sawLoading = true
            sawLoading && canvasState.todayCanvas != null -> firstPaintDone = true
        }
    }

    // 연출은 로딩 덮개가 걷힌 뒤에 시작한다(그룹 캔버스와 같다)
    var isCovered by remember { mutableStateOf(true) }

    // 백그라운드에서 돌아오면 재생 중이던 빈 캔버스 연출을 정리한다
    LifecycleStartEffect(viewModel) {
        viewModel.processIntent(SoloCanvasIntent.AppStarted)
        onStopOrDispose { }
    }

    Box(modifier = modifier) {
        YGScaffoldV2(
            modifier = Modifier.fillMaxSize(),
            isLoading = canvasState.isInitialLoading || (firstPaintDone.not() && loadState != CanvasLoadState.Loaded),
            onCoveredChange = { isCovered = it },
            loadingOverlay = {
                if (loadState == CanvasLoadState.Failed) {
                    CanvasLoadErrorOverlay(onClickRetry = { retryKey++ })
                } else {
                    CanvasLoadingOverlay()
                }
            },
        ) { innerPadding ->
            CanvasMainScreen(
                canvasState = canvasState,
                onClickBack = { navigator.onBack() },
                onClickDateSelect = null,
                onClickMenu = { viewModel.processIntent(SoloCanvasIntent.ClickMenu) },
                onClickCamera = { viewModel.processIntent(SoloCanvasIntent.ClickCamera) },
                onClickGallery = { viewModel.processIntent(SoloCanvasIntent.ClickGallery) },
                onClickTextInput = { viewModel.processIntent(SoloCanvasIntent.ClickText) },
                onClickEditCanvasBG = { navigator.goTo(destination = NavKeySoloBGEdit) },
                onClickSaveAsImage = {
                    pendingSaveType = CanvasSaveType.IMAGE
                    viewModel.processIntent(SoloCanvasIntent.ClickSaveToGallery)
                },
                onClickSaveAsVideo = {
                    pendingSaveType = CanvasSaveType.VIDEO
                    viewModel.processIntent(SoloCanvasIntent.ClickSaveToGallery)
                },
                // 혼자 공간의 토핑은 전부 내 것이라 스포트라이트 없이 바로 재편집으로 간다
                onClickTopping = { topping ->
                    navigator.goTo(
                        destination = NavKeySoloToppingArrange(
                            initialToppingId = topping.parfaitImageId.value,
                            initialIsText = false,
                        ),
                    )
                },
                onClickText = { text ->
                    navigator.goTo(
                        destination = NavKeySoloToppingArrange(
                            initialToppingId = text.parfaitTextId.value,
                            initialIsText = true,
                        ),
                    )
                },
                // 혼자 공간은 늘 오늘을 보고 달력이 없어 아래 콜백이 불릴 일이 없다
                onClickGoToToday = {},
                onDismissCalendar = {},
                onSelectYear = {},
                onSelectMonth = {},
                onClickDate = {},
                onClickSpotlightDim = {},
                isEmptyIntroPaused = isCovered,
                onEmptyIntroTap = { viewModel.processIntent(SoloCanvasIntent.EmptyIntroTap) },
                onEmptyIntroInteraction = { viewModel.processIntent(SoloCanvasIntent.EmptyIntroInteraction) },
                onEmptyIntroAnimationFinished = {
                    viewModel.processIntent(SoloCanvasIntent.EmptyIntroAnimationFinished)
                },
                onEmptyIntroDismissFinished = { viewModel.processIntent(SoloCanvasIntent.EmptyIntroDismissFinished) },
                emptyMessageRes = R.string.solo_canvas_empty_message,
                emptyHighlightRes = R.string.solo_canvas_empty_highlight,
                onLoadStateChange = { loadState = it },
                retryKey = retryKey,
                toastPolicy = toastPolicy,
                graphicsLayer = graphicsLayer,
                onCanvasAreaSizeChange = { width, height ->
                    canvasAreaWidth = width
                    canvasAreaHeight = height
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        SoloDialogs(
            soloDialog = uiState.soloDialog,
            onIntent = viewModel::processIntent,
        )
    }
}

@Composable
private fun SoloDialogs(
    soloDialog: SoloDialog?,
    onIntent: (SoloCanvasIntent) -> Unit,
) {
    val dismiss = { onIntent(SoloCanvasIntent.DismissDialog) }

    when (soloDialog) {
        SoloDialog.Menu -> {
            // 시스템 뒤로가기는 메뉴만 닫는다
            BackHandler(onBack = dismiss)
            YGScaffoldV2(containerColor = YGAtomicColors.Transparency.Black25) { innerPadding ->
                SoloSpaceMenu(
                    onClickReset = { onIntent(SoloCanvasIntent.ClickResetMenu) },
                    onClickDelete = { onIntent(SoloCanvasIntent.ClickDeleteMenu) },
                    onDismiss = dismiss,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        // 왼쪽(secondary)이 그만두기, 오른쪽(primary)이 실행이다
        SoloDialog.ResetConfirm -> YGModalPopup(
            title = stringResource(R.string.solo_canvas_reset_dialog_title),
            body = stringResource(R.string.solo_canvas_reset_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.solo_canvas_dialog_cancel),
            onSecondaryClick = dismiss,
            primaryText = stringResource(R.string.solo_canvas_reset_dialog_confirm),
            onPrimaryClick = { onIntent(SoloCanvasIntent.ConfirmReset) },
            onDismissRequest = dismiss,
        )

        SoloDialog.DeleteConfirm -> YGModalPopup(
            title = stringResource(R.string.solo_canvas_delete_dialog_title),
            body = stringResource(R.string.solo_canvas_delete_dialog_body),
            iconRes = DesignSystemR.drawable.ic_warning_round,
            secondaryText = stringResource(R.string.solo_canvas_dialog_cancel),
            onSecondaryClick = dismiss,
            primaryText = stringResource(R.string.solo_canvas_delete_dialog_confirm),
            onPrimaryClick = { onIntent(SoloCanvasIntent.ConfirmDelete) },
            onDismissRequest = dismiss,
        )

        null -> Unit
    }
}

private fun PendingGallerySave.toSaveIntent(): SoloCanvasIntent = when (this) {
    is PendingGallerySave.Image -> SoloCanvasIntent.SaveCapturedCanvas(bitmap)
    is PendingGallerySave.Video -> SoloCanvasIntent.SaveRecordedVideo(filePath)
}
