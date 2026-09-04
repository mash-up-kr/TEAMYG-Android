package com.teamyg.parfait.feature.groups.canvas.impl.route

import android.content.ClipData
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygalert.rememberYGAlertPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.util.android.permission.GalleryWritePermissionManager
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadErrorOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadingOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasToppingLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.util.toSpotlightTimeLabel
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
import kotlinx.coroutines.launch
import kotlinx.datetime.number

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

    // 이 화면의 토스트는 전부 캔버스 프레임 상단에 뜬다 — 작성자 알림이 그 자리에 고정돼 있고,
    // 실패만 화면 최상단으로 보내면 같은 화면에서 자리가 갈린다. 큐를 하나로 둬야 Toast 공통
    // 정책의 스택(나중 것이 위로)도 성립한다. 그래서 스캐폴드에는 정책을 넘기지 않는다
    val toastPolicy = rememberYGToastPolicy()
    // 환영 배너는 Alert 한 자리만 쓴다 — 지난 캔버스 알림(TODO)이 실제로 붙기 전까지는 이 화면에서
    // Alert 를 여기서만 띄운다
    val alertPolicy = rememberYGAlertPolicy()
    val gallerySaveSuccessFormat = stringResource(R.string.canvas_main_gallery_save_success)
    val gallerySaveFailureMessage = stringResource(R.string.canvas_main_gallery_save_failure)
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
        if (granted && bitmap != null) {
            viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(bitmap))
        } else if (bitmap != null) {
            toastPolicy.showError(gallerySaveFailureMessage)
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

                is CanvasMainEffect.RequestCanvasCapture -> {
                    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                    if (GalleryWritePermissionManager.hasPermission(context)) {
                        viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(bitmap))
                    } else {
                        pendingGalleryBitmap = bitmap
                        galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
                    }
                }

                is CanvasMainEffect.ShowGallerySaveResult -> if (effect.isSuccess) {
                    val message = gallerySaveSuccessFormat.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(gallerySaveFailureMessage)
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
            }
        }
    }

    // Spotlight 상태에서 앱이 백그라운드로 이동했다가 돌아오면 Default 로 복귀한다
    LifecycleStartEffect(viewModel) {
        viewModel.processIntent(CanvasMainIntent.OnAppReturnedFromBackground)
        onStopOrDispose { }
    }

    // 캔버스 영역만 덮으면 그 밖의 날짜 선택과 메뉴가 그대로 눌린다
    var loadState by remember { mutableStateOf(CanvasToppingLoadState.Loaded) }

    var retryKey by remember { mutableIntStateOf(0) }

    YGScaffoldV2(
        modifier = modifier,
        isLoading = canvasState.isInitialLoading || loadState != CanvasToppingLoadState.Loaded,
        loadingOverlay = {
            if (loadState == CanvasToppingLoadState.Failed) {
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
}
