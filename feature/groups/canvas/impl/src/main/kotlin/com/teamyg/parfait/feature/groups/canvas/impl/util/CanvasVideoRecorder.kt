package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.teamyg.parfait.core.util.android.video.Mp4VideoEncoder
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoRecordOptions
import com.teamyg.parfait.feature.groups.canvas.impl.model.VideoTimelineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

// 공유 FileProvider 가 캐시 전체가 아니라 이 하위 디렉터리만 열도록 전용 폴더에 둔다(canvas_share_file_paths)
private const val VIDEO_CACHE_DIR_NAME = "canvas_video"
private const val VIDEO_CACHE_FILE_PREFIX = "canvas_timelapse_"
private const val VIDEO_CACHE_FILE_SUFFIX = ".mp4"

// 저장·공유로 넘긴 mp4 는 받는 쪽(갤러리 복사·인스타그램)이 언제 다 읽었는지 알 수 없어 바로 지우지 못한다.
// 이보다 오래된 것만 다음 녹화가 치운다
private const val STALE_VIDEO_MILLIS = 60 * 60 * 1000L

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
    val directory = File(context.cacheDir, VIDEO_CACHE_DIR_NAME).apply { mkdirs() }
    withContext(Dispatchers.IO) { deleteStaleVideos(directory) }
    // 녹화마다 새 이름을 쓴다. 고정 이름이면 취소된 이전 녹화의 close() 가 새 녹화 파일을 지우거나,
    // 아직 갤러리·인스타그램이 읽는 파일을 새 녹화가 덮어쓴다
    val output = File(directory, VIDEO_CACHE_FILE_PREFIX + UUID.randomUUID() + VIDEO_CACHE_FILE_SUFFIX)

    // 인코더 호출은 부른 스레드를 막는다(Mp4VideoEncoder KDoc). 메인에 두면 만드는 동안 화면이
    // 멈추므로 레이어 읽기만 메인에 남기고 나머지는 넘긴다
    val encoder = runInterruptible(Dispatchers.Default) {
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
            var encoded = false
            try {
                // 인코더가 멈춰도 취소(시간 초과)가 대기를 끊도록 인터럽트를 허용한다
                runInterruptible(Dispatchers.Default) { encoder.encodeFrame(bitmap) }
                encoded = true
            } finally {
                // 인터럽트는 대기만 끊고 인코더 스레드의 작업은 계속 돈다. 중단으로 빠져나왔으면 그 스레드가
                // 아직 이 비트맵을 그리는 중일 수 있어 재활용하지 않고 GC 에 맡긴다
                if (encoded) bitmap.recycle()
            }
        }

        runInterruptible(Dispatchers.Default) { encoder.finish() }
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

private fun deleteStaleVideos(directory: File) {
    val threshold = System.currentTimeMillis() - STALE_VIDEO_MILLIS
    directory
        .listFiles()
        ?.filter { it.name.endsWith(VIDEO_CACHE_FILE_SUFFIX) && it.lastModified() < threshold }
        ?.forEach { it.delete() }
}
