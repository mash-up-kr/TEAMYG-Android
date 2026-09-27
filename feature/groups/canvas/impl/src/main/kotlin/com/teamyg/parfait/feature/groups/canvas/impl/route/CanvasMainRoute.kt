package com.teamyg.parfait.feature.groups.canvas.impl.route

import android.content.ClipData
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import com.teamyg.parfait.core.designsystem.component.ygalert.rememberYGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.component.ygcanvas.YGCanvasBackground
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.util.android.permission.GalleryWritePermissionManager
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_IMAGE_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasImageSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadErrorOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadingOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasTutorialOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasVideoCaptureHost
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.screen.toYGCanvasBackground
import com.teamyg.parfait.feature.groups.canvas.impl.util.CANVAS_VIDEO_FINAL_HOLD_FRAMES
import com.teamyg.parfait.feature.groups.canvas.impl.util.CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceSnapshot
import com.teamyg.parfait.feature.groups.canvas.impl.util.canvasVideoRevealSteps
import com.teamyg.parfait.feature.groups.canvas.impl.util.readCanvasCaptureCache
import com.teamyg.parfait.feature.groups.canvas.impl.util.toSpotlightTimeLabel
import com.teamyg.parfait.feature.groups.canvas.impl.util.writeToCanvasCaptureCache
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyCameraCustom
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasMainIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasWelcome
import com.teamyg.parfait.feature.gallery.api.NavKeyCustomGalleryPicker
import com.teamyg.parfait.feature.gallery.api.RecentImagePick
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasBGEdit
import com.teamyg.parfait.feature.groups.setting.api.NavKeyGroupSetting
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.ui.R as CoreUiR
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.number
import java.io.File
import java.io.FileOutputStream

private const val CLIP_LABEL_INVITE_MESSAGE = "invite_message"

/** 작성자 정보는 지금 강조된 토핑의 것 하나만 보여야 한다. */
private const val SPOTLIGHT_TOAST_TAG = "spotlight"

@Composable
internal fun CanvasMainRoute(
    navKey: NavKeyCanvasMain,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasMainViewModel = hiltViewModel(
        creationCallback = { factory: CanvasMainViewModel.Factory ->
            factory.create(
                groupIdValue = navKey.groupId,
                welcomeGroupName = navKey.welcomeGroupName,
                welcomeInviteCode = navKey.welcomeInviteCode,
            )
        },
    ),
) {
    val canvasState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    val videoGraphicsLayer = rememberGraphicsLayer()
    var activeVideoSnapshot by remember { mutableStateOf<CanvasVideoSourceSnapshot?>(null) }
    val videoRevealProgress = remember { mutableStateMapOf<ParfaitImageId, Float>() }
    var pendingVideoCapture by remember { mutableStateOf(false) }

    // 이 화면의 토스트는 전부 캔버스 프레임 상단에 뜬다 — 작성자 알림이 그 자리에 고정돼 있고,
    // 실패만 화면 최상단으로 보내면 같은 화면에서 자리가 갈린다. 큐를 하나로 둬야 Toast 공통
    // 정책의 스택(나중 것이 위로)도 성립한다. 그래서 스캐폴드에는 정책을 넘기지 않는다
    val toastPolicy = rememberYGToastPolicy()
    // 환영 배너와 지난 캔버스 알림이 Alert 한 자리를 같이 쓴다 — 겹치지 않는다. 지난 캔버스
    // 알림은 이 기기·이 그룹 조합을 처음 확인할 때는 절대 뜨지 않는데(기존 사용자를 위한
    // 알림이라 기준선만 세운다), 환영 배너는 정확히 그 "처음 확인하는 순간"에만 뜬다
    val alertPolicy = rememberYGAlertPolicy()
    val gallerySaveSuccessFormat = stringResource(R.string.canvas_main_gallery_save_success)
    val gallerySaveFailureMessage = stringResource(R.string.canvas_main_gallery_save_failure)
    val captureFailureMessage = stringResource(R.string.canvas_main_capture_failure)
    val videoCaptureFailureMessage = stringResource(R.string.canvas_main_video_capture_failure)
    val gallerySaveVideoSuccessFormat = stringResource(R.string.canvas_main_video_save_success)
    val gallerySaveVideoFailureMessage = stringResource(R.string.canvas_main_video_save_failure)
    val closedCanvasAlertTitleFormat = stringResource(R.string.canvas_main_closed_canvas_alert_title)
    val closedCanvasAlertSubFormat = stringResource(R.string.canvas_main_closed_canvas_alert_sub)
    val closedCanvasAlertButtonText = stringResource(R.string.canvas_main_closed_canvas_alert_button)
    val todayCanvasErrorMessage = stringResource(R.string.canvas_main_today_canvas_error)
    val toppingFlowStartErrorMessage = stringResource(R.string.canvas_main_topping_flow_start_error)
    val welcomeJoinedTitleFormat = stringResource(R.string.canvas_welcome_joined_title)
    val welcomeJoinedSub = stringResource(R.string.canvas_welcome_joined_sub)
    val welcomeCreatedTitleFormat = stringResource(R.string.canvas_welcome_created_title)
    val welcomeCreatedSubFormat = stringResource(R.string.canvas_welcome_created_sub)
    val welcomeInviteCopyText = stringResource(R.string.canvas_welcome_invite_copy)
    val welcomeInviteCopiedText = stringResource(R.string.canvas_welcome_invite_copied)
    val inviteMessageTemplate = stringResource(CoreUiR.string.group_invite_message)

    // WRITE_EXTERNAL_STORAGE 요청은 Activity 가 있어야만 가능해, 캡처한 비트맵을 여기서
    // 들고 있다가 승인이 오면 그때 ViewModel 로 넘긴다(API 29+ 는 애초에 필요 없어 안 걸린다)
    var pendingGalleryBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val galleryWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val bitmap = pendingGalleryBitmap
        pendingGalleryBitmap = null
        val videoRequested = pendingVideoCapture
        pendingVideoCapture = false

        if (granted && bitmap != null) {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(bitmap))
        } else if (granted && videoRequested) {
            scope.launch {
                runCanvasVideoCapture(
                    context = context,
                    videoGraphicsLayer = videoGraphicsLayer,
                    onSnapshotActive = { activeVideoSnapshot = it },
                    onRevealProgressChange = { id, progress -> videoRevealProgress[id] = progress },
                    onFailure = { toastPolicy.showError(videoCaptureFailureMessage) },
                    onFramesReady = { frames, outputFile ->
                        viewModel.processIntent(
                            CanvasMainIntent.EncodeAndSaveCanvasVideo(frames = frames, outputFile = outputFile),
                        )
                    },
                )
            }
        } else if (bitmap != null || videoRequested) {
            toastPolicy.showError(gallerySaveFailureMessage)
        }
    }

    // 갤러리 저장은 권한을 물어야 시작할 수 있다 — 미리보기에서 돌아온 길과 권한 승인 뒤의
    // 길이 같은 곳으로 모이도록 여기 한 번만 적는다
    val saveWithPermission: (Bitmap) -> Unit = { bitmap ->
        if (GalleryWritePermissionManager.hasPermission(context)) {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(bitmap))
        } else {
            pendingGalleryBitmap = bitmap
            galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
        }
    }

    // 미리보기에서 저장을 확정하고 돌아왔다. 보여 준 그림 그대로 남겨야 하므로 캔버스를 다시
    // 캡처하지 않고 미리보기가 쓰던 파일을 읽는다
    ResultEffect<CanvasImageSaveResult>(resultKey = CANVAS_IMAGE_SAVE_RESULT_KEY) { result ->
        scope.launch {
            withContext(Dispatchers.IO) { readCanvasCaptureCache(result.imagePath) }
                .onSuccess(saveWithPermission)
                .onFailure { toastPolicy.showError(gallerySaveFailureMessage) }
        }
    }

    // 백스택 아래에 깔린 엔트리는 컴포지션에서 빠지므로 다시 앞에 설 때 한 번 더 돈다.
    // 매번 다시 묻는 이유는 CanvasMainIntent.Enter 에 있다
    LifecycleResumeEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.Enter)
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CanvasMainEffect.NavigateToCamera -> navigator.goTo(
                    destination = NavKeyCameraCustom(),
                )

                // 원본까지 실으면 한 흐름이 남긴 두 장이 같은 사진으로 나란히 뜬다(OQ-P-258)
                is CanvasMainEffect.NavigateToCanvas -> navigator.goTo(
                    destination = NavKeyCustomGalleryPicker(recentImagePick = RecentImagePick.CUTOUT),
                )

                is CanvasMainEffect.NavigateToCanvasBGEdit -> navigator.goTo(
                    destination = NavKeyCanvasBGEdit(
                        groupId = effect.groupId.value,
                        parfaitId = effect.parfaitId.value,
                        initialToppingId = effect.toppingId?.value,
                    ),
                )

                is CanvasMainEffect.NavigateToGroupSetting -> navigator.goTo(
                    destination = NavKeyGroupSetting(groupId = effect.groupId.value),
                )

                is CanvasMainEffect.RequestCanvasCaptureForPreview -> {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    val selectedDate = viewModel.state.value.selectedDate

                    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
                        .onSuccess { file ->
                            // ⚠️ 홀더에 건넸다고 파일 쓰기를 지우면 안 된다 — 위 ResultEffect 가
                            // 저장할 때 그 파일을 읽는다
                            CanvasCaptureHolder.put(bitmap)
                            CanvasVideoSourceHolder.put(
                                CanvasVideoSourceSnapshot(
                                    background = viewModel.state.value.canvasBackground.toYGCanvasBackground(),
                                    toppings = viewModel.state.value.toppings,
                                ),
                            )
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                ),
                            )
                        }.onFailure { toastPolicy.showError(captureFailureMessage) }
                }

                is CanvasMainEffect.ShowGallerySaveResult -> if (effect.isSuccess) {
                    val message = gallerySaveSuccessFormat.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(gallerySaveFailureMessage)
                }

                is CanvasMainEffect.RequestCanvasVideoCapture -> {
                    if (GalleryWritePermissionManager.hasPermission(context)) {
                        scope.launch {
                            runCanvasVideoCapture(
                                context = context,
                                videoGraphicsLayer = videoGraphicsLayer,
                                onSnapshotActive = { activeVideoSnapshot = it },
                                onRevealProgressChange = { id, progress -> videoRevealProgress[id] = progress },
                                onFailure = { toastPolicy.showError(videoCaptureFailureMessage) },
                                onFramesReady = { frames, outputFile ->
                                    viewModel.processIntent(
                                        CanvasMainIntent.EncodeAndSaveCanvasVideo(
                                            frames = frames,
                                            outputFile = outputFile,
                                        ),
                                    )
                                },
                            )
                        }
                    } else {
                        pendingVideoCapture = true
                        galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
                    }
                }

                is CanvasMainEffect.ShowGalleryVideoSaveResult -> if (effect.isSuccess) {
                    val message = gallerySaveVideoSuccessFormat.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(gallerySaveVideoFailureMessage)
                }

                is CanvasMainEffect.ShowSpotlightToast -> toastPolicy.show(
                    type = YGToastType.Record(
                        userName = effect.nickname,
                        time = effect.elapsed.toSpotlightTimeLabel(context),
                        userNameColor = effect.nicknameColor,
                    ),
                    replaceTag = SPOTLIGHT_TOAST_TAG,
                )

                is CanvasMainEffect.ShowTodayCanvasError -> toastPolicy.showError(todayCanvasErrorMessage)

                is CanvasMainEffect.ShowToppingFlowStartError ->
                    toastPolicy.showError(toppingFlowStartErrorMessage)

                is CanvasMainEffect.ShowWelcome -> when (val welcome = effect.welcome) {
                    is CanvasWelcome.Joined -> alertPolicy.show(
                        title = welcomeJoinedTitleFormat.format(welcome.groupName),
                        sub = welcomeJoinedSub,
                    )

                    is CanvasWelcome.Created -> alertPolicy.show(
                        title = welcomeCreatedTitleFormat.format(welcome.groupName),
                        sub = welcomeCreatedSubFormat.format(welcome.inviteCode),
                        buttonText = welcomeInviteCopyText,
                        buttonIconResource = DesignSystemR.drawable.ic_copy,
                        onButtonClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(
                                        ClipData.newPlainText(
                                            CLIP_LABEL_INVITE_MESSAGE,
                                            inviteMessageTemplate.format(welcome.inviteCode),
                                        ),
                                    ),
                                )
                            }
                            // 복사 확인 문구로 바꿔 다시 띄운다 — 같은 배너를 새 타이머로 한 번 더 보여준다
                            alertPolicy.show(
                                title = welcomeCreatedTitleFormat.format(welcome.groupName),
                                sub = welcomeCreatedSubFormat.format(welcome.inviteCode),
                                buttonText = welcomeInviteCopiedText,
                                buttonIconResource = DesignSystemR.drawable.ic_copy,
                            )
                        },
                    )
                }

                is CanvasMainEffect.ShowPastCanvasAlert -> alertPolicy.show(
                    title = closedCanvasAlertTitleFormat.format(effect.date.month.number, effect.date.day),
                    sub = closedCanvasAlertSubFormat.format(effect.memberCount),
                    buttonText = closedCanvasAlertButtonText,
                    onButtonClick = {
                        viewModel.processIntent(CanvasMainIntent.ClickPastCanvasAlertDate(effect.date))
                    },
                )
            }
        }
    }

    // Spotlight 상태에서 앱이 백그라운드로 이동했다가 돌아오면 Default 로 복귀한다
    LifecycleStartEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.OnAppReturnedFromBackground)
        onStopOrDispose { }
    }

    // 캔버스 영역만 덮으면 그 밖의 날짜 선택과 메뉴가 그대로 눌린다
    var loadState by remember { mutableStateOf(CanvasLoadState.Loaded) }

    var retryKey by remember { mutableIntStateOf(0) }

    // 폴링이 남이 올린 토핑을 실어 올 때마다 보고 있던 캔버스가 덮개 뒤로 사라져서다
    val displayedCanvasId = canvasState.displayedCanvas?.parfaitId
    var paintedCanvasIds by remember { mutableStateOf(emptySet<ParfaitId>()) }
    var sawLoading by remember { mutableStateOf(false) }
    var observedCanvasId by remember { mutableStateOf(displayedCanvasId) }

    val firstPaintDone = displayedCanvasId != null && displayedCanvasId in paintedCanvasIds

    LaunchedEffect(displayedCanvasId, canvasState.isInitialLoading, loadState) {
        // 아직 그리지 못한 다른 캔버스로 옮겨 왔을 때만 관측을 처음부터 다시 모은다. 이미 그린
        // 캔버스로 돌아온 것(달력에서 지난 날을 보다 오늘로)은 리셋이 아니다 — 토핑도 배경도
        // 없는 캔버스는 loadState 가 Loaded 를 안 벗어나 한 번 버린 관측을 다시 얻지 못한다
        val movedToUnpaintedCanvas = displayedCanvasId != null &&
            observedCanvasId != null &&
            displayedCanvasId != observedCanvasId &&
            displayedCanvasId !in paintedCanvasIds
        if (movedToUnpaintedCanvas) {
            sawLoading = false
        }
        if (displayedCanvasId != null) {
            observedCanvasId = displayedCanvasId
        }

        // loadState 는 아직 아무 이미지도 안 붙은 첫 컴포지션에서도 Loaded 다. 로딩을 한 번
        // 본 뒤로 좁히지 않으면 캐시된 캔버스로 들어올 때 덮개가 아예 안 뜬다
        when {
            canvasState.isInitialLoading || loadState != CanvasLoadState.Loaded -> sawLoading = true
            sawLoading && displayedCanvasId != null -> paintedCanvasIds = paintedCanvasIds + displayedCanvasId
        }
    }

    // 튜토리얼은 스캐폴드 **밖**에 겹친다 — 안에 넣으면 컨텐츠 인셋을 받아 딤이 상태바
    // 밑에서 끊기고, 시스템바만 안 덮인 화면이 된다
    Box(modifier = modifier.fillMaxSize()) {
        YGScaffoldV2(
            isLoading = canvasState.isInitialLoading || (firstPaintDone.not() && loadState != CanvasLoadState.Loaded),
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
                onClickDateSelect = { viewModel.processIntent(CanvasMainIntent.OnClickDateSelect) },
                onClickMenu = { viewModel.processIntent(CanvasMainIntent.OnClickGroupSetting) },
                onClickCamera = { viewModel.processIntent(CanvasMainIntent.OnClickCamera()) },
                onClickGallery = { viewModel.processIntent(CanvasMainIntent.OnClickCanvas()) },
                onClickEditCanvasBG = { viewModel.processIntent(CanvasMainIntent.OnClickCanvasEdit()) },
                onClickSaveToGallery = { viewModel.processIntent(CanvasMainIntent.OnClickSaveToGallery) },
                onClickGoToToday = { viewModel.processIntent(CanvasMainIntent.OnClickGoToToday) },
                onDismissCalendar = { viewModel.processIntent(CanvasMainIntent.DismissCalendar) },
                onSelectYear = { viewModel.processIntent(CanvasMainIntent.SelectYear(it)) },
                onSelectMonth = { viewModel.processIntent(CanvasMainIntent.SelectMonth(it)) },
                onClickDate = { viewModel.processIntent(CanvasMainIntent.ClickDate(it)) },
                onClickTopping = { viewModel.processIntent(CanvasMainIntent.OnClickTopping(it)) },
                onClickSpotlightDim = { viewModel.processIntent(CanvasMainIntent.OnClickSpotlightDim) },
                onLoadStateChange = { loadState = it },
                retryKey = retryKey,
                toastPolicy = toastPolicy,
                alertPolicy = alertPolicy,
                graphicsLayer = graphicsLayer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        canvasState.tutorialStep?.let { step ->
            CanvasTutorialOverlay(
                step = step,
                onClickNext = { viewModel.processIntent(CanvasMainIntent.OnClickTutorialNext) },
            )
        }

        activeVideoSnapshot?.let { snapshot ->
            CanvasVideoCaptureHost(
                background = snapshot.background,
                toppings = snapshot.toppings,
                revealProgress = videoRevealProgress,
                graphicsLayer = videoGraphicsLayer,
                modifier = Modifier.offset(x = 10_000.dp),
            )
        }
    }
}

/**
 * 배경·토핑 이미지를 먼저 메모리 캐시에 올려 둔다 — 프레임을 여러 장 그리는 동안 이미지가
 * 늦게 도착하면 일부 프레임만 비어 보인다(정지 이미지 캡처의 OQ-P-272와 같은 결함이 프레임
 * 수만큼 반복되는 것을 막는다).
 */
private suspend fun preloadCanvasVideoImages(
    context: Context,
    snapshot: CanvasVideoSourceSnapshot,
): Result<Unit> = runCatching {
    val urls = buildList {
        (snapshot.background as? YGCanvasBackground.Image)?.let { add(it.url) }
        addAll(snapshot.toppings.map { it.imageUrl })
    }

    urls.forEach { url ->
        val result = context.imageLoader.execute(ImageRequest.Builder(context).data(url).build())
        check(result is SuccessResult) { "이미지를 미리 불러오지 못했다: $url" }
    }
}

/**
 * [CanvasVideoSourceHolder] 스냅샷을 오프스크린으로 재생하며 프레임을 순서대로 캡처해 캐시에
 * PNG로 쓴다. 배경만 있는 시작 프레임 → 토핑마다 [CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING] 단계로
 * 페이드인+슬라이드인 → 마지막에 [CANVAS_VIDEO_FINAL_HOLD_FRAMES] 만큼 정지 프레임을 더한다.
 */
private suspend fun runCanvasVideoCapture(
    context: Context,
    videoGraphicsLayer: GraphicsLayer,
    onSnapshotActive: (CanvasVideoSourceSnapshot?) -> Unit,
    onRevealProgressChange: (ParfaitImageId, Float) -> Unit,
    onFailure: () -> Unit,
    onFramesReady: (frames: List<File>, outputFile: File) -> Unit,
) {
    val snapshot = CanvasVideoSourceHolder.peek()
    if (snapshot == null) {
        onFailure()
        return
    }

    if (preloadCanvasVideoImages(context, snapshot).isFailure) {
        onFailure()
        return
    }

    val frameDir = File(context.cacheDir, "canvas_video_frames").apply { mkdirs() }
    val frames = mutableListOf<File>()

    suspend fun captureFrame() {
        // state 반영 → 레이아웃 → 드로우까지 실제로 한 번 돌 시간을 준다
        withFrameNanos {}
        withFrameNanos {}
        val bitmap = videoGraphicsLayer.toImageBitmap().asAndroidBitmap()
        val frameFile = File(frameDir, "frame_${frames.size}.png")
        FileOutputStream(frameFile).use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output) }
        frames += frameFile
    }

    try {
        onSnapshotActive(snapshot)

        captureFrame() // 배경만 있는 시작 프레임 — 모든 토핑이 revealProgress 0

        for (topping in snapshot.toppings) {
            for (progress in canvasVideoRevealSteps(CANVAS_VIDEO_REVEAL_STEPS_PER_TOPPING)) {
                onRevealProgressChange(topping.parfaitImageId, progress)
                captureFrame()
            }
        }

        repeat(CANVAS_VIDEO_FINAL_HOLD_FRAMES) { captureFrame() }

        val outputFile = File(context.cacheDir, "canvas_video_${System.currentTimeMillis()}.mp4")
        onFramesReady(frames, outputFile)
    } catch (throwable: Throwable) {
        frames.forEach { it.delete() }
        onFailure()
    } finally {
        onSnapshotActive(null)
    }
}
