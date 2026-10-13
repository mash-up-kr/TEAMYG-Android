package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.teamyg.parfait.core.util.android.video.Mp4VideoEncoder
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoRecordOptions
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

private const val VIDEO_CACHE_FILE_NAME = "canvas_timelapse.mp4"

/**
 * 캔버스 타임랩스 mp4 를 캐시에 만든다.
 *
 * 프레임 하나를 만드는 일은 **상태를 한 칸 전진시키고 그려질 때까지 기다린 뒤 레이어를 읽는
 * 것**이다. 그 세 단계가 화면 계층에 묶여 있어 이 함수는 호출부에서 받은 [advanceFrame] 으로
 * 앞의 둘을 위임하고, 읽기와 인코딩만 맡는다.
 *
 * 벽시계를 보지 않는다 — 프레임 번호가 타임스탬프를 정하므로 느린 기기에서도 재생 속도가 같다.
 *
 * @param advanceFrame 프레임 인덱스를 받아 그 상태가 실제로 **그려질 때까지** 대기한다.
 *   호출부가 `withFrameNanos` 로 프레임 경계를 기다려야 한다
 * @return 완성된 mp4 파일. 실패하면 `Result.failure` 이고 부분 파일은 남지 않는다.
 *   **취소는 실패로 접지 않는다** — 화면을 떠난 것이 "저장 실패" 토스트로 둔갑하면 안 된다
 */
internal suspend fun recordCanvasVideo(
    context: Context,
    frameCount: Int,
    captureLayer: GraphicsLayer,
    advanceFrame: suspend (frameIndex: Int) -> Unit,
): Result<File> = runCatching {
    val output = File(context.cacheDir, VIDEO_CACHE_FILE_NAME)

    // 인코더 호출은 부른 스레드를 막는다(Mp4VideoEncoder KDoc). 메인에 두면 만드는 동안 화면이
    // 멈추므로 레이어 읽기만 메인에 남기고 나머지는 넘긴다
    val encoder = withContext(Dispatchers.Default) {
        Mp4VideoEncoder(
            outputFile = output,
            width = VideoRecordOptions.WIDTH_PX,
            height = VideoRecordOptions.HEIGHT_PX,
            framesPerSecond = VideoTimelineOptions.FRAMES_PER_SECOND,
        )
    }

    try {
        for (frameIndex in 0 until frameCount) {
            advanceFrame(frameIndex)

            val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
            try {
                withContext(Dispatchers.Default) { encoder.encodeFrame(bitmap) }
            } finally {
                bitmap.recycle()
            }
        }

        withContext(Dispatchers.Default) { encoder.finish() }
    } finally {
        // 취소된 뒤에도 닫아야 코덱이 풀리고 부분 파일이 지워진다
        withContext(NonCancellable + Dispatchers.Default) { encoder.close() }
    }

    // moov 박스는 close() 가 쓴다 — 닫힌 **뒤에야** 재생 가능한 파일이다
    output
}.onFailure { throwable ->
    // runCatching 이 CancellationException 까지 접으므로 다시 던진다
    if (throwable is CancellationException) throw throwable
}
