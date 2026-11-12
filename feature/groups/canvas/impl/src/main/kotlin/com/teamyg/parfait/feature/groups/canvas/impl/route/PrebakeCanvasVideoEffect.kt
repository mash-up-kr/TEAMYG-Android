package com.teamyg.parfait.feature.groups.canvas.impl.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.teamyg.parfait.core.ui.screenLogger
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoRecordOptions
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import com.teamyg.parfait.feature.groups.canvas.impl.util.CanvasLoadState
import com.teamyg.parfait.feature.groups.canvas.impl.util.recordCanvasVideo
import com.teamyg.parfait.feature.groups.canvas.impl.util.withForegroundTimeoutOrNull
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageSaveIntent
import com.teamyg.parfait.feature.groups.canvas.impl.viewmodel.CanvasImageSaveViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException

/**
 * 미리 굽기: 영상 미리보기로 들어오는 순간 mp4 를 굽기 시작한다. 구경만 하고 나가면 코루틴이 취소되고
 * 부분 파일은 지워진다(recordCanvasVideo). 녹화는 컴포지션 프레임에 묶여 있어 화면 계층에서만 돈다.
 *
 * 구성 변경으로 효과가 다시 돌면 ViewModel 상태를 보고 이미 구운 경우를 거른다
 *
 * @param canRecord 굽는 데 쓸 재료가 있는지. 프로세스 재시작으로 홀더가 비었거나 캔버스를 재지 못했으면 거짓
 * @param loadState 녹화 레이어가 배경과 토핑을 다 불러왔는지. 첫 프레임을 빈 그림으로 찍지 않으려고 기다린다
 */
@Composable
internal fun PrebakeCanvasVideoEffect(
    viewModel: CanvasImageSaveViewModel,
    canRecord: Boolean,
    recordLayer: GraphicsLayer,
    loadState: () -> CanvasLoadState,
    onFrame: (Int) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(Unit) {
        if (viewModel.state.value.needsRecording
                .not()
        ) {
            return@LaunchedEffect
        }

        if (canRecord.not()) {
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            return@LaunchedEffect
        }

        viewModel.processIntent(CanvasImageSaveIntent.StartRecording)

        val settledLoadState = withTimeoutOrNull(VideoRecordOptions.SOURCE_LOAD_TIMEOUT_MILLIS) {
            snapshotFlow(loadState).first { it != CanvasLoadState.Loading }
        }

        if (settledLoadState != CanvasLoadState.Loaded) {
            viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            return@LaunchedEffect
        }

        val result = withForegroundTimeoutOrNull(lifecycle, VideoRecordOptions.RECORD_TIMEOUT_MILLIS) {
            recordCanvasVideo(
                context = context,
                frameCount = VideoTimelineOptions.FRAME_COUNT,
                captureLayer = recordLayer,
            ) { frameIndex ->
                onFrame(frameIndex)
                // 상태를 세운 뒤 그려질 때까지 기다린다. **두 번**인 이유: awaiter 는
                // Choreographer 의 ANIMATION 콜백에서 깨는데 draw 는 그 뒤 TRAVERSAL 에서
                // 돈다 — 한 번이면 매 프레임이 한 칸씩 밀린다
                withFrameNanos { }
                withFrameNanos { }
            }
        } ?: Result.failure(TimeoutException("캔버스 영상 녹화 시간 초과"))

        result.fold(
            onSuccess = { viewModel.processIntent(CanvasImageSaveIntent.RecordingSucceeded(it)) },
            onFailure = { throwable ->
                screenLogger.e(throwable) { "PrebakeCanvasVideoEffect - 캔버스 영상 녹화 실패" }
                viewModel.processIntent(CanvasImageSaveIntent.RecordingFailed)
            },
        )
    }
}
