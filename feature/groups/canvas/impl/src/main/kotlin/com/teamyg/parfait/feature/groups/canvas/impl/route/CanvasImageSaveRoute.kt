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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.addLastModifiedToFileCacheKey
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
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageSaveEffect
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageSaveIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageSaveViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import java.io.File

private const val IMAGE_MIME_TYPE = "image/*"
private const val VIDEO_MIME_TYPE = "video/*"

/**
 * navKey 인자와 전역 홀더로 그린다. 갤러리에 넣는 일은 결과를 받는 캔버스 메인의 몫이다.
 *
 * 한 라우트가 [CanvasSaveType] 으로 사진·영상 미리보기를 겸한다. 사진은 캡처한 PNG 를 정적으로
 * 보이고, 영상은 저장될 타임랩스를 반복 재생한다. 영상은 들어오는 즉시 mp4 를 미리 굽어 저장·공유를
 * 즉시 처리한다
 */
@Composable
internal fun CanvasImageSaveRoute(
    navKey: NavKeyCanvasImageSave,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CanvasImageSaveViewModel = hiltViewModel(),
) {
    val resultEventBus = LocalResultEventBus.current
    val context = LocalContext.current
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    val capturedBitmap = remember { CanvasCaptureHolder.peek()?.asImageBitmap() }
    val videoSource = remember { CanvasVideoSourceHolder.peek() }
    val isVideo = navKey.saveType == CanvasSaveType.VIDEO

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
                viewModel.fileToDeleteOnExit()?.delete()
            }
        }
    }

    // 미리 굽기: 영상 미리보기로 들어오는 순간 시작한다. 구경만 하고 나가면 코루틴이 취소되고
    // 부분 파일은 지워진다(recordCanvasVideo). 녹화 자체는 화면 계층에 묶여 여기서만 돈다
    LaunchedEffect(Unit) {
        if (isVideo.not()) return@LaunchedEffect

        // 재료가 없거나(프로세스 재시작) 캔버스를 재지 못했으면 만들 수 없다
        if (videoSource == null || videoSource.canvasWidth <= 0.dp) {
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            return@LaunchedEffect
        }

        viewModel.processIntent(CanvasImageSaveIntent.StartRecording)

        // 첫 프레임을 빈 그림으로 찍지 않는다 — 배경과 토핑 이미지가 다 모여야 시작한다
        val settledLoadState = withTimeoutOrNull(VideoRecordOptions.SOURCE_LOAD_TIMEOUT_MILLIS) {
            snapshotFlow { recordLoadState }.first { it != CanvasLoadState.Loading }
        }

        if (settledLoadState != CanvasLoadState.Loaded) {
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
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
            onSuccess = { viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(it)) },
            onFailure = { throwable ->
                screenLogger.e(throwable) { "CanvasImageSaveRoute - 캔버스 영상 녹화 실패" }
                viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            },
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                // 저장 결과는 캔버스 메인이 토스트로 알린다
                is CanvasImageSaveEffect.DispatchSave -> {
                    resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, effect.result)
                    navigator.onBack()
                }

                // 띄웠으면 성공 토스트는 없다. 못 띄웠으면(인스타 미설치 등) 실패를 캔버스 메인이
                // 알린다 — 여긴 곧 사라지는 화면이라 토스트를 걸 자리가 없다
                is CanvasImageSaveEffect.DispatchShare -> {
                    val shared = shareCanvasToInstagramStory(context, effect.file, VIDEO_MIME_TYPE)
                    if (shared.not()) {
                        resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, CanvasSaveResult.ShareFailed)
                    }
                    navigator.onBack()
                }

                // 영상을 만들지 못해 공유로 넘기지 못했다
                CanvasImageSaveEffect.DispatchBack -> {
                    resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, CanvasSaveResult.ShareFailed)
                    navigator.onBack()
                }
            }
        }
    }

    // 동작을 누른 뒤 mp4 를 기다리는 동안엔 나가면 결과를 알릴 길이 없다. 미리 굽는 중(동작 전)에는
    // 자유롭게 나갈 수 있다
    BackHandler(enabled = uiState.isBusy) {}

    Box(modifier = modifier.fillMaxSize()) {
        YGScaffoldV2 { innerPadding ->
            CanvasImageSaveScreen(
                saveType = navKey.saveType,
                date = LocalDate.parse(navKey.date),
                isSaveEnabled = uiState.isBusy.not(),
                isShareEnabled = uiState.isBusy.not(),
                onClickClose = { if (uiState.isBusy.not()) navigator.onBack() },
                onClickSave = {
                    if (isVideo) {
                        viewModel.processIntent(CanvasImageSaveIntent.RequestSave)
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
                        viewModel.processIntent(CanvasImageSaveIntent.RequestShare)
                    } else {
                        // 띄웠으면 성공 토스트는 없다. 못 띄웠으면 실패를 캔버스 메인이 알린다
                        val shared = shareCanvasToInstagramStory(
                            context = context,
                            file = File(navKey.imagePath),
                            mimeType = IMAGE_MIME_TYPE,
                        )
                        if (shared.not()) {
                            resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, CanvasSaveResult.ShareFailed)
                        }
                        navigator.onBack()
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
                    CanvasImagePreview(bitmap = capturedBitmap, fallbackImagePath = navKey.imagePath)
                }
            }
        }

        // 미리 굽는 동안에만 컴포지션에 붙는다. 화면에는 한 픽셀도 그리지 않고 프레임만 기록한다.
        // 0dp 로는 밀도를 정할 수 없어(0 으로 나눈다) 재지 못한 재료로는 붙이지 않는다
        if (uiState.isRecording && videoSource != null && videoSource.canvasWidth > 0.dp) {
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
private fun CanvasImagePreview(
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
