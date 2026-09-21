package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.teamyg.parfait.core.util.android.video.Mp4VideoEncoder
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/** 산출물 가로 픽셀. 캔버스 영역이 세로 9:16 이라 높이는 이 값의 16/9 다 */
internal const val CANVAS_VIDEO_WIDTH_PX = 720
internal const val CANVAS_VIDEO_HEIGHT_PX = 1280

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
 *   **취소는 실패로 접지 않는다** — 화면을 떠나 코루틴이 취소된 것을 실패로 돌려주면 호출부가
 *   실패 이펙트를 이펙트 채널에 남기고, 그것이 다음 재진입 때 엉뚱한 토스트로 뜬다
 */
internal suspend fun recordCanvasVideo(
    context: Context,
    frameCount: Int,
    captureLayer: GraphicsLayer,
    advanceFrame: suspend (frameIndex: Int) -> Unit,
): Result<File> = runCatching {
    val output = File(context.cacheDir, VIDEO_CACHE_FILE_NAME)

    Mp4VideoEncoder(
        outputFile = output,
        width = CANVAS_VIDEO_WIDTH_PX,
        height = CANVAS_VIDEO_HEIGHT_PX,
        framesPerSecond = CANVAS_VIDEO_FRAMES_PER_SECOND,
    ).use { encoder ->
        for (frameIndex in 0 until frameCount) {
            advanceFrame(frameIndex)

            val bitmap = captureLayer.toImageBitmap().asAndroidBitmap()
            // 디스패처를 갈아 부르지 않는다. 인코더는 전용 단일 스레드를 소유하고 EGL 컨텍스트가
            // 그 스레드에 묶여 있어, 내부에서 스스로 그 스레드로 넘긴다(Mp4VideoEncoder KDoc)
            encoder.encodeFrame(bitmap)
            bitmap.recycle()
        }

        encoder.finish()
    }

    // moov 박스는 finish() 가 아니라 close() 가 쓴다 — use 블록이 닫힌 **뒤에야** 재생 가능한
    // 파일이라, 산출물을 호출부로 넘기는 것도 블록 밖인 여기서 한다(Mp4VideoEncoder KDoc)
    output
}.onFailure { throwable ->
    // 취소는 실패가 아니다. runCatching 이 CancellationException 까지 접어 버리므로 여기서
    // 다시 던져, 화면을 떠난 것이 "영상 저장 실패" 토스트로 둔갑하지 않게 한다.
    // use 블록의 close() 는 이미 돌아 부분 파일도 지워진 상태다
    if (throwable is CancellationException) throw throwable
}
