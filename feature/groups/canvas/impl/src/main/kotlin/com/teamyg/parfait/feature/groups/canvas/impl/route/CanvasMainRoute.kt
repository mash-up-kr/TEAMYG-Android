package com.teamyg.parfait.feature.groups.canvas.impl.route

import android.content.ClipData
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.teamyg.parfait.core.designsystem.component.ygloading.YGLoadingOverlay
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.util.android.permission.GalleryWritePermissionManager
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_IMAGE_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadErrorOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasLoadingOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasTutorialOverlay
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasMainScreen
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.readCanvasCaptureCache
import com.teamyg.parfait.feature.groups.canvas.impl.util.recordCanvasVideo
import com.teamyg.parfait.feature.groups.canvas.impl.util.toSpotlightTimeLabel
import com.teamyg.parfait.feature.groups.canvas.impl.util.toppingVideoFrames
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // 이 컴포지션이 실제로 "새로 서는" 순간의 값을 여기, 컴포지션 본문에서 붙잡아 둔다.
    // ⚠️ 아래 LaunchedEffect 본문 안에서 viewModel.state.value 를 다시 읽으면 안 된다 — 이
    // 화면의 유일한 정상 녹화 시작 경로인 ResultEffect(OnClickSaveVideoToGallery)는 미리보기
    // 화면에서 "영상으로 저장"을 눌러 돌아온, 이 Route 의 새 컴포지션의 **첫 재개**에서 곧바로
    // isRecordingVideo=true 를 세운다(NavDisplay 가 백스택 아래 엔트리를 컴포지션에서 빼므로,
    // 미리보기에서 돌아오는 것 자체가 이 Route 를 처음부터 다시 세우는 일이다 — 아래 canvasState
    // 폴링부의 "백스택 아래 깔린 엔트리는 컴포지션에서 빠진다" 주석 참고). 즉 정상 녹화도
    // "이 컴포지션이 막 서는 시점"에 시작될 수 있다 — ①의 최초 판별 논거였던 "정상 녹화는
    // 화면이 이미 떠 있던 도중에만 시작된다"는 전제가 이 경로에서는 성립하지 않는다.
    // 컴포지션 안 이펙트들은 선언 순서를 따르는 FIFO 로 실행되므로, 만약 저 아래 복구용
    // LaunchedEffect 본문 안에서 state 를 읽는다면 ResultEffect → 효과 수집기 → 복구 검사
    // 순으로 같은 최초 실행 배치에서 돌면서, 복구 검사가 ResultEffect 가 방금 세운 새 값을
    // "이전 컴포지션이 남긴 stale 값"으로 오판해 막 시작한 정상 녹화를 그 자리에서 취소해
    // 버린다. `remember{}` 는 어떤 `LaunchedEffect` 본문보다도 먼저, 컴포지션 단계에서
    // 평가되므로 여기서 값을 붙잡아 두면 그 뒤 어떤 이펙트가 state 를 바꾸든 이 값은
    // "컴포지션이 서던 바로 그 순간"에 고정된다 — 이 값이 참이면 이 컴포지션이 시작되기도
    // 전에, 즉 완전히 다른 죽은 컴포지션에서 이미 녹화 중이었다는 뜻이라 유실로 확정할 수
    // 있다. 두 경우(막 시작한 녹화 vs 유실된 녹화)를 "언제 검사했는가"라는 타이밍이 아니라
    // "어디서 읽었는가"라는 구조로 가르는 것이 이 필드의 존재 이유다 — "본문에서 읽으나
    // 이펙트 안에서 읽으나 어차피 같은 값 아니냐"고 보고 이 줄을 지우면 안 된다
    val wasRecordingAtEntry = remember { viewModel.state.value.isRecordingVideo }

    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

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
    val gallerySaveVideoSuccessFormat = stringResource(R.string.canvas_main_gallery_video_save_success)
    val gallerySaveVideoFailureMessage = stringResource(R.string.canvas_main_gallery_video_save_failure)
    val captureFailureMessage = stringResource(R.string.canvas_main_capture_failure)
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

    // WRITE_EXTERNAL_STORAGE 요청은 Activity 가 있어야만 가능해, 저장할 것을 여기서 들고 있다가
    // 승인이 오면 그때 ViewModel 로 넘긴다(API 29+ 는 애초에 필요 없어 안 걸린다).
    // 이미지는 비트맵, 영상은 파일 경로라 한 타입으로는 못 담는다
    var pendingGallerySave by remember { mutableStateOf<PendingGallerySave?>(null) }
    val galleryWritePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val pending = pendingGallerySave
        pendingGallerySave = null
        when (pending) {
            null -> Unit

            is PendingGallerySave.Image -> if (granted) {
                viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(pending.bitmap))
            } else {
                toastPolicy.showError(gallerySaveFailureMessage)
            }

            // 영상은 거부도 인텐트로 돌려보낸다 — 토스트만 띄우면 isRecordingVideo 가 남아
            // 로딩 오버레이가 영원히 걷히지 않는다
            is PendingGallerySave.Video -> if (granted) {
                viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(pending.filePath))
            } else {
                viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
            }
        }
    }

    // 갤러리 저장은 권한을 물어야 시작할 수 있다 — 미리보기에서 돌아온 길과 권한 승인 뒤의
    // 길이 같은 곳으로 모이도록 여기 한 번만 적는다
    val saveWithPermission: (PendingGallerySave) -> Unit = { pending ->
        if (GalleryWritePermissionManager.hasPermission(context)) {
            when (pending) {
                is PendingGallerySave.Image ->
                    viewModel.processIntent(CanvasMainIntent.SaveCapturedCanvas(pending.bitmap))

                is PendingGallerySave.Video ->
                    viewModel.processIntent(CanvasMainIntent.SaveRecordedVideo(pending.filePath))
            }
        } else {
            pendingGallerySave = pending
            galleryWritePermissionLauncher.launch(GalleryWritePermissionManager.PERMISSION)
        }
    }

    // 녹화는 컴포지션을 프레임마다 읽는 일이라 화면 계층 책임이다 — 프레임 상태를 여기서 들고
    // CanvasMainScreen 의 녹화 레이어로 내려보낸다
    val recordLayer = rememberGraphicsLayer()
    var recordVisibleToppingCount by remember { mutableIntStateOf(0) }
    var recordLastToppingPopProgress by remember { mutableFloatStateOf(1f) }
    // 배경과 토핑을 접은 신호다. 배경 이미지가 붙기 전에 첫 프레임을 찍지 않으려면 둘이 함께
    // 들어와야 한다 — 접는 일은 녹화 레이어가 한다
    var recordLoadState by remember { mutableStateOf(CanvasLoadState.Loading) }
    var canvasAreaWidth by remember { mutableStateOf(0.dp) }
    var canvasAreaHeight by remember { mutableStateOf(0.dp) }

    // 미리보기에서 저장을 확정하고 돌아왔다. 이미지는 보여 준 그림 그대로 남겨야 하므로 캔버스를
    // 다시 캡처하지 않고 미리보기가 쓰던 파일을 읽는다. 동영상은 평면 PNG 로는 만들 수 없어
    // ViewModel 이 든 토핑 목록에서 다시 그린다
    ResultEffect<CanvasSaveResult>(resultKey = CANVAS_IMAGE_SAVE_RESULT_KEY) { result ->
        when (result) {
            is CanvasSaveResult.Image -> scope.launch {
                withContext(Dispatchers.IO) { readCanvasCaptureCache(result.imagePath) }
                    .onSuccess { bitmap -> saveWithPermission(PendingGallerySave.Image(bitmap)) }
                    .onFailure { toastPolicy.showError(gallerySaveFailureMessage) }
            }

            is CanvasSaveResult.Video ->
                viewModel.processIntent(CanvasMainIntent.OnClickSaveVideoToGallery)
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
                    // 캡처와 같은 시점의 값이어야 한다 — 아래 정지 지점(파일 쓰기) 사이 폴링으로
                    // 다른 그룹원이 토핑을 올리거나 지우면, 나중에 읽은 개수가 이미 캡처된
                    // 그림과 어긋난다
                    val toppingCount = viewModel.state.value.toppings.size

                    withContext(Dispatchers.IO) { bitmap.writeToCanvasCaptureCache(context) }
                        .onSuccess { file ->
                            // ⚠️ 홀더에 건넸다고 파일 쓰기를 지우면 안 된다 — 위 ResultEffect 가
                            // 저장할 때 그 파일을 읽는다
                            CanvasCaptureHolder.put(bitmap)
                            navigator.goTo(
                                destination = NavKeyCanvasImageSave(
                                    imagePath = file.absolutePath,
                                    date = selectedDate.toString(),
                                    toppingCount = toppingCount,
                                ),
                            )
                        }.onFailure { toastPolicy.showError(captureFailureMessage) }
                }

                is CanvasMainEffect.StartCanvasVideoRecording -> {
                    val frames = toppingVideoFrames(
                        toppingCount = viewModel.state.value.toppings.size,
                    )

                    // 캔버스를 아직 재지 못했으면 녹화 레이어를 띄울 수 없다(밀도를 0 으로
                    // 나눈다). 아래 대기까지 가면 붙지 않을 레이어를 로딩 오버레이 밑에서
                    // 영원히 기다리므로, 대기 **전에** 끊는다
                    if (canvasAreaWidth <= 0.dp) {
                        viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                    } else {
                        // 첫 프레임을 빈 그림으로 찍지 않는다 — 토핑 이미지가 다 모여야 시작한다
                        val settledLoadState = snapshotFlow { recordLoadState }
                            .first { it != CanvasLoadState.Loading }

                        if (settledLoadState == CanvasLoadState.Failed) {
                            viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                        } else {
                            recordCanvasVideo(
                                context = context,
                                frameCount = frames.size,
                                captureLayer = recordLayer,
                            ) { frameIndex ->
                                val frame = frames[frameIndex]
                                recordVisibleToppingCount = frame.visibleCount
                                recordLastToppingPopProgress = frame.popProgress
                                // 상태를 세운 뒤 실제로 그려진 프레임을 기다린다. 이 대기가
                                // 없으면 직전 상태를 두 번 찍는다.
                                // **두 번** 부르는 이유: AndroidUiDispatcher 는 Choreographer 의
                                // ANIMATION 콜백에서 awaiter 를 깨우는데 Compose 의 draw 는 그
                                // 뒤인 TRAVERSAL 콜백에서 돈다. 한 번만 기다리면 아직 그려지지
                                // 않은 시점에 레이어를 읽어 매 프레임이 한 칸씩 밀린다
                                withFrameNanos { }
                                withFrameNanos { }
                            }.onSuccess { file ->
                                saveWithPermission(PendingGallerySave.Video(file.absolutePath))
                            }.onFailure {
                                viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
                            }
                        }
                    }

                    // 녹화가 끝나면 레이어가 컴포지션에서 빠진다. 여기서 되돌리지 않으면 다음
                    // 녹화가 남아 있는 Loaded 를 보고 새 레이어의 첫 보고를 기다리지 않는다
                    recordLoadState = CanvasLoadState.Loading
                    recordVisibleToppingCount = 0
                    recordLastToppingPopProgress = 1f
                }

                // 저장 경로는 이미지와 영상이 같고 문구만 갈린다
                is CanvasMainEffect.ShowGallerySaveResult -> if (effect.isSuccess) {
                    val format = if (effect.isVideo) gallerySaveVideoSuccessFormat else gallerySaveSuccessFormat
                    val message = format.format(effect.date.month.number, effect.date.day)
                    toastPolicy.show(YGToastType.InviteCode(message))
                } else {
                    toastPolicy.showError(
                        if (effect.isVideo) gallerySaveVideoFailureMessage else gallerySaveFailureMessage,
                    )
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

    // wasRecordingAtEntry(위 선언부의 긴 주석 참고)가 참이면, 그 값을 세운 주체는 이 컴포지션이
    // 아니라 "이전에 살아 있던" 다른 컴포지션이다 — 이 컴포지션이 서기도 전에 이미 참이었다는
    // 뜻이기 때문이다. 즉 녹화를 몰던 컴포지션이(Activity 재생성·백스택 이탈 등으로) 죽었고,
    // 그 StartCanvasVideoRecording 이펙트는 죽은 컴포지션의 수집기가 이미 소비해 두 번 다시
    // 오지 않는다(BaseViewModel.effect KDoc — Channel 은 한 번 소비된 이펙트를 재전달하지
    // 않는다). 유실된 녹화를 실패로 접어 로딩 오버레이를 걷는다. LaunchedEffect(Unit) 으로
    // 이 컴포지션 생애주기당 딱 한 번만 검사한다 — 리컴포지션마다 다시 돌면 막 시작한 정상
    // 녹화까지 실패로 접어 버린다. ⚠️ 여기서 viewModel.state.value 를 다시 읽지 않는다 —
    // 그러면 wasRecordingAtEntry 를 둔 의미가 없어져 위에서 막은 회귀가 되돌아온다
    LaunchedEffect(Unit) {
        if (wasRecordingAtEntry) {
            viewModel.processIntent(CanvasMainIntent.CanvasVideoRecordFailed)
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
                recordLayer = recordLayer,
                recordVisibleToppingCount = recordVisibleToppingCount,
                recordLastToppingPopProgress = recordLastToppingPopProgress,
                recordCanvasWidth = canvasAreaWidth,
                recordCanvasHeight = canvasAreaHeight,
                onCanvasAreaSizeChange = { width, height ->
                    canvasAreaWidth = width
                    canvasAreaHeight = height
                },
                onRecordLoadStateChange = { recordLoadState = it },
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

        // 녹화 중에는 화면을 덮는다. 오버레이가 클릭을 삼켜 별도 차단 장치가 필요 없다.
        // CanvasLoadingOverlay 가 아니라 문구 없는 공통 로딩을 쓰는 이유: 그쪽은 "캔버스를
        // 불러오는 중"이라고 말하는데, 영상을 만드는 중에 그 문구는 거짓 안내다
        if (canvasState.isRecordingVideo) {
            YGLoadingOverlay()
        }
    }
}

/** 권한 승인을 기다리는 저장 대상. 이미지와 영상이 서로 다른 것을 나르므로 갈라 둔다 */
private sealed interface PendingGallerySave {
    data class Image(val bitmap: Bitmap) : PendingGallerySave

    data class Video(val filePath: String) : PendingGallerySave
}
