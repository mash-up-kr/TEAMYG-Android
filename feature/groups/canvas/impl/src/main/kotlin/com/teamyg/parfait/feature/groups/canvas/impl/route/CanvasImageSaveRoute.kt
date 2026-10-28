package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.ui.screenLogger
import com.teamyg.parfait.feature.groups.canvas.api.CANVAS_SAVE_RESULT_KEY
import com.teamyg.parfait.feature.groups.canvas.api.CanvasSaveResult
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasImageSave
import com.teamyg.parfait.feature.groups.canvas.impl.component.CanvasVideoRecordLayer
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoRecordOptions
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import com.teamyg.parfait.feature.groups.canvas.impl.screen.CanvasImageSaveScreen
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasCaptureHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasVideoSourceHolder
import com.teamyg.parfait.feature.groups.canvas.impl.util.recordCanvasVideo
import com.teamyg.parfait.feature.groups.canvas.impl.util.visibleToppingCount
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

/**
 * ViewModel 이 없다 — navKey 인자와 전역 홀더만 읽어 그리면 되고, 갤러리에 넣는 일은 결과를
 * 받는 캔버스 메인의 몫이다.
 *
 * 영상은 여기서 **만들기까지** 한다(ADR-0035). 만드는 동안 화면은 그대로 두고 버튼과 닫기만 막는다.
 */
@Composable
internal fun CanvasImageSaveRoute(
    navKey: NavKeyCanvasImageSave,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val resultEventBus = LocalResultEventBus.current
    val context = LocalContext.current

    val capturedBitmap = remember { CanvasCaptureHolder.peek()?.asImageBitmap() }
    val videoSource = remember { CanvasVideoSourceHolder.peek() }

    var isRecordingVideo by remember { mutableStateOf(false) }
    val recordLayer = rememberGraphicsLayer()
    var recordVisibleToppingCount by remember { mutableIntStateOf(0) }
    // 배경과 토핑을 접은 신호다. 접는 일은 녹화 레이어가 한다
    var recordLoadState by remember { mutableStateOf(CanvasLoadState.Loading) }

    DisposableEffect(navKey) {
        onDispose {
            // 키가 남아 있으면 잠깐 내려간 것이라 돌아왔을 때 그림이 있어야 한다
            if (navKey !in navigator.backStack) {
                CanvasCaptureHolder.clear()
                CanvasVideoSourceHolder.clear()
            }
        }
    }

    // 만드는 중에 나가면 결과를 알릴 길이 없다
    BackHandler(enabled = isRecordingVideo) {}

    LaunchedEffect(isRecordingVideo) {
        if (isRecordingVideo.not()) return@LaunchedEffect

        // 재료가 없거나(프로세스 재시작) 캔버스를 재지 못했으면 녹화 레이어가 붙지 않는다.
        // 아래 대기까지 가면 오지 않을 보고를 영원히 기다리므로 대기 **전에** 끊는다
        val result = if (videoSource == null || videoSource.canvasWidth <= 0.dp) {
            CanvasSaveResult.VideoFailed
        } else {
            // 첫 프레임을 빈 그림으로 찍지 않는다 — 배경과 토핑 이미지가 다 모여야 시작한다
            val settledLoadState = withTimeoutOrNull(VideoRecordOptions.SOURCE_LOAD_TIMEOUT_MILLIS) {
                snapshotFlow { recordLoadState }.first { it != CanvasLoadState.Loading }
            }

            if (settledLoadState != CanvasLoadState.Loaded) {
                CanvasSaveResult.VideoFailed
            } else {
                recordCanvasVideo(
                    context = context,
                    frameCount = VideoTimelineOptions.FRAME_COUNT,
                    captureLayer = recordLayer,
                ) { frameIndex ->
                    recordVisibleToppingCount = visibleToppingCount(
                        toppingCount = videoSource.toppings.size + videoSource.texts.size,
                        frameIndex = frameIndex,
                    )
                    // 상태를 세운 뒤 그려질 때까지 기다린다. **두 번**인 이유: awaiter 는
                    // Choreographer 의 ANIMATION 콜백에서 깨는데 draw 는 그 뒤 TRAVERSAL 에서
                    // 돈다 — 한 번이면 매 프레임이 한 칸씩 밀린다
                    withFrameNanos { }
                    withFrameNanos { }
                }.fold(
                    onSuccess = { file -> CanvasSaveResult.Video(videoPath = file.absolutePath) },
                    onFailure = { throwable ->
                        screenLogger.e(throwable) { "CanvasImageSaveRoute - 캔버스 영상 녹화 실패" }
                        CanvasSaveResult.VideoFailed
                    },
                )
            }
        }

        resultEventBus.sendResult(CANVAS_SAVE_RESULT_KEY, result)
        navigator.onBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        YGScaffoldV2 { innerPadding ->
            CanvasImageSaveScreen(
                bitmap = capturedBitmap,
                fallbackImagePath = navKey.imagePath,
                date = LocalDate.parse(navKey.date),
                isImageSaveEnabled = isRecordingVideo.not(),
                isVideoSaveEnabled = navKey.toppingCount > 0 && isRecordingVideo.not(),
                onClickClose = { if (isRecordingVideo.not()) navigator.onBack() },
                onClickSaveImage = {
                    resultEventBus.sendResult(
                        CANVAS_SAVE_RESULT_KEY,
                        CanvasSaveResult.Image(imagePath = navKey.imagePath),
                    )
                    navigator.onBack()
                },
                onClickSaveVideo = { isRecordingVideo = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        // 만드는 중에만 컴포지션에 붙는다. 화면에는 한 픽셀도 그리지 않고 프레임만 기록한다.
        // 0dp 로는 밀도를 정할 수 없어(0 으로 나눈다) 재지 못한 재료로는 붙이지 않는다
        if (isRecordingVideo && videoSource != null && videoSource.canvasWidth > 0.dp) {
            CanvasVideoRecordLayer(
                toppings = videoSource.toppings,
                texts = videoSource.texts,
                background = videoSource.background,
                visibleToppingCount = recordVisibleToppingCount,
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
