package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.LocalResultEventBus
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.screenLogger
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveType
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasVideoPreview
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasVideoRecordLayer
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoRecordOptions
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasImageSaveScreen
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.recordCanvasVideo
import com.teamyg.parfait.feature.groups.canvas.impl.util.shareCanvasToInstagramStory
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasVideoSaveState
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.PreviewOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import java.io.File

private const val IMAGE_MIME_TYPE = "image/*"
private const val VIDEO_MIME_TYPE = "video/*"

/**
 * ViewModel 이 없다 — navKey 인자와 전역 홀더만 읽어 그리면 되고, 갤러리에 넣는 일은 결과를
 * 받는 캔버스 메인의 몫이다.
 *
 * 한 라우트가 [CanvasSaveType] 으로 사진·영상 미리보기를 겸한다. 사진은 캡처한 PNG 를 정적으로
 * 보이고, 영상은 저장될 타임랩스를 반복 재생한다. 영상은 들어오는 즉시 mp4 를 미리 굽어 저장·공유를 즉시 처리한다
 */
@Composable
internal fun CanvasImageSaveRoute(
    navKey: NavKeyCanvasImageSave,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current
    val context = LocalContext.current
    // 공유 실패(인스타 미설치·실행 실패·영상 못 만듦)는 미리보기에서 알려 준다 — 그냥 닫히면
    // 사용자가 이유를 모른다
    val toastPolicy = rememberYGToastPolicy()
    val shareFailedMessage = stringResource(R.string.canvas_share_failed)

    val capturedBitmap = remember { CanvasCaptureHolder.peek()?.asImageBitmap() }
    val videoSource = remember { CanvasVideoSourceHolder.peek() }
    val isVideo = navKey.saveType == CanvasSaveType.VIDEO

    // 저장·공유의 상태 기계·파일 수명 판단은 홀더가 든다
    val saveState = remember { CanvasVideoSaveState() }
    val recordLayer = rememberGraphicsLayer()
    var recordFrameIndex by remember { mutableIntStateOf(0) }
    // 배경과 토핑을 접은 신호다. 접는 일은 녹화 레이어가 한다
    var recordLoadState by remember { mutableStateOf(CanvasLoadState.Loading) }

    DisposableEffect(navKey) {
        onDispose {
            // 키가 남아 있으면 잠깐 내려간 것이라 돌아왔을 때 그림이 있어야 한다
            if (navKey !in navigator.backStack) {
                CanvasCaptureHolder.clear()
                CanvasVideoSourceHolder.clear()
                // 저장·공유로 넘기지 않은 미리 구운 mp4 는 캐시에 남기지 않는다
                saveState.fileToDeleteOnExit()?.delete()
            }
        }
    }

    // 미리 굽기: 영상 미리보기로 들어오는 순간 시작한다. 구경만 하고 나가면 코루틴이 취소되고
    // 부분 파일은 지워진다(recordCanvasVideo). 녹화 자체는 화면 계층에 묶여 여기서만 돈다
    LaunchedEffect(Unit) {
        if (isVideo.not()) return@LaunchedEffect

        // 재료가 없거나(프로세스 재시작) 캔버스를 재지 못했으면 만들 수 없다
        if (videoSource == null || videoSource.canvasWidth <= 0.dp) {
            saveState.recordingFailed()
            return@LaunchedEffect
        }

        saveState.startRecording()

        // 첫 프레임을 빈 그림으로 찍지 않는다 — 배경과 토핑 이미지가 다 모여야 시작한다
        val settledLoadState = withTimeoutOrNull(VideoRecordOptions.SOURCE_LOAD_TIMEOUT_MILLIS) {
            snapshotFlow { recordLoadState }.first { it != CanvasLoadState.Loading }
        }

        if (settledLoadState != CanvasLoadState.Loaded) {
            saveState.recordingFailed()
            return@LaunchedEffect
        }

        recordCanvasVideo(
            context = context,
            frameCount = VideoTimelineOptions.FRAME_COUNT,
            captureLayer = recordLayer,
        ) { frameIndex ->
            recordFrameIndex = frameIndex
            // 상태를 세운 뒤 그려질 때까지 기다린다. **두 번**인 이유: awaiter 는
            // Choreographer 의 ANIMATION 콜백에서 깨는데 draw 는 그 뒤 TRAVERSAL 에서
            // 돈다 — 한 번이면 매 프레임이 한 칸씩 밀린다
            withFrameNanos { }
            withFrameNanos { }
        }.fold(
            onSuccess = { saveState.recordingSucceeded(it) },
            onFailure = { throwable ->
                screenLogger.e(throwable) { "CanvasImageSaveRoute - 캔버스 영상 녹화 실패" }
                saveState.recordingFailed()
            },
        )
    }

    // 저장·공유 실행: 동작과 mp4 가 둘 다 준비되면 홀더가 내보낼 것을 돌려준다.
    LaunchedEffect(saveState.phase, saveState.isBusy) {
        when (val outcome = saveState.dispatch()) {
            null -> Unit

            // 저장 결과는 캔버스 메인이 토스트로 알린다
            is PreviewOutcome.Save -> {
                resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, outcome.result)
                navigator.onBack()
            }

            // 띄웠으면 성공 토스트 없이 조용히 돌아간다. 못 띄우면(인스타 미설치 등) 이유를 알리고
            // 화면은 유지해 다시 시도하거나 닫을 수 있게 한다
            is PreviewOutcome.Share ->
                if (shareCanvasToInstagramStory(context, outcome.file, VIDEO_MIME_TYPE)) {
                    saveState.markShared()
                    navigator.onBack()
                } else {
                    saveState.cancelPendingAction()
                    toastPolicy.showError(shareFailedMessage)
                }

            // 영상을 만들지 못해 공유로 넘기지 못했다 — 알리고 화면은 유지한다
            PreviewOutcome.Back -> {
                saveState.cancelPendingAction()
                toastPolicy.showError(shareFailedMessage)
            }
        }
    }

    // 동작을 누른 뒤 mp4 를 기다리는 동안엔 나가면 결과를 알릴 길이 없다. 미리 굽는 중(동작 전)에는
    // 자유롭게 나갈 수 있다
    BackHandler(enabled = saveState.isBusy) {}

    Box(modifier = modifier.fillMaxSize()) {
        YGScaffoldV2(toastPolicy = toastPolicy) { innerPadding ->
            CanvasImageSaveScreen(
                saveType = navKey.saveType,
                date = LocalDate.parse(navKey.date),
                isSaveEnabled = saveState.isBusy.not(),
                isShareEnabled = saveState.isBusy.not(),
                onClickClose = { if (saveState.isBusy.not()) navigator.onBack() },
                onClickSave = {
                    if (isVideo) {
                        saveState.requestSave()
                    } else {
                        resultEventBus.sendResult(
                            CANVAS_SAVE_RESULT_KEY,
                            CanvasSaveResult.Image(imagePath = navKey.imagePath),
                        )
                        navigator.onBack()
                    }
                },
                onClickShareInstagram = {
                    if (isVideo) {
                        saveState.requestShare()
                    } else if (shareCanvasToInstagramStory(context, File(navKey.imagePath), IMAGE_MIME_TYPE)) {
                        // 띄웠으면 성공 토스트 없이 조용히 돌아간다
                        navigator.onBack()
                    } else {
                        // 못 띄웠으면(인스타 미설치 등) 알리고 화면은 유지한다
                        toastPolicy.showError(shareFailedMessage)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // 영상 재료가 있으면 저장될 타임랩스를 반복 재생한다. 사진이거나 재료를 못 받았으면
                // 캡처한 그림을 정적으로 보인다
                if (isVideo && videoSource != null && videoSource.canvasWidth > 0.dp) {
                    CanvasVideoPreview(
                        toppings = videoSource.toppings,
                        texts = videoSource.texts,
                        background = videoSource.background,
                        retryKey = videoSource.retryKey,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    StaticCanvasPreview(bitmap = capturedBitmap, fallbackImagePath = navKey.imagePath)
                }
            }
        }

        // 미리 굽는 동안에만 컴포지션에 붙는다. 화면에는 한 픽셀도 그리지 않고 프레임만 기록한다.
        // 0dp 로는 밀도를 정할 수 없어(0 으로 나눈다) 재지 못한 재료로는 붙이지 않는다
        if (saveState.isRecording && videoSource != null && videoSource.canvasWidth > 0.dp) {
            CanvasVideoRecordLayer(
                toppings = videoSource.toppings,
                texts = videoSource.texts,
                background = videoSource.background,
                frameIndex = recordFrameIndex,
                canvasWidth = videoSource.canvasWidth,
                canvasHeight = videoSource.canvasHeight,
                targetWidthPx = VideoRecordOptions.WIDTH_PX,
                retryKey = videoSource.retryKey,
                captureLayer = recordLayer,
                onLoadStateChange = { recordLoadState = it },
            )
        }
    }
}

/** 캡처한 캔버스를 정적으로 보인다. 비트맵이 있으면 그대로, 없으면 캐시 PNG 경로로 불러온다 */
@Composable
private fun StaticCanvasPreview(
    bitmap: ImageBitmap?,
    fallbackImagePath: String,
) {
    val previewDescription = stringResource(R.string.canvas_image_save_preview_content_description)

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = previewDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
        AsyncImage(
            model = ImageRequest
                .Builder(LocalContext.current)
                .data(fallbackImagePath)
                // 캡처 파일명이 고정이라(CanvasCaptureCache) 경로만으로는 캐시 키가 안 갈린다 —
                // 다시 저장한 캔버스를 열어도 이전 캡처가 뜰 수 있다
                .addLastModifiedToFileCacheKey(true)
                .build(),
            contentDescription = previewDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
