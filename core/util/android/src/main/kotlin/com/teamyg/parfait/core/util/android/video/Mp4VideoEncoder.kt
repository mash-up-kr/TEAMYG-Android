package com.teamyg.parfait.core.util.android.video

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import com.teamyg.parfait.core.util.android.coreUtilAndroidLogger
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 비트맵 시퀀스를 무음 mp4 로 쓴다.
 *
 * 프레임 타임스탬프를 벽시계가 아니라 **프레임 번호 ÷ [framesPerSecond]** 로 계산한다 —
 * 인코딩이 느린 기기에서도 재생 속도가 같아야 한다.
 *
 * [finish] 를 부르지 않고 닫으면 **산출물을 지운다.** 헤더가 덜 쓰인 mp4 는 재생할 수 없는데,
 * 남겨 두면 호출부가 그것을 갤러리에 올릴 수 있다.
 *
 * **어느 스레드에서 호출해도 안전하지만, 그 호출부 스레드를 동기적으로 막는다.** 내부에서 쓰는
 * [BitmapSurfaceWriter] 는 자신을 생성한 스레드에만 묶이는 EGL 컨텍스트를 갖는다(그 클래스
 * KDoc 참고) — 다른 스레드에서 `draw` 를 부르면 예외 없이 그리기가 조용히 실패한다. 그래서 이
 * 클래스는 생성자부터 [encodeFrame]·[finish]·[close] 까지 전부 스스로 만든 전용 단일 스레드
 * 위에서만 실행하고, [runOnEncoderThread] 가 그 작업이 끝날 때까지 호출부를 `Future.get()` 으로
 * 동기 대기시킨다. 생성자를 포함한 이유는, 문제가 되는 건 `draw` 뿐이지만 `BitmapSurfaceWriter`
 * 를 만드는 스레드가 곧 그 EGL 컨텍스트가 묶이는 스레드이기 때문이다 — 생성과 draw 를 다른
 * 스레드에서 하면 어차피 draw 가 실패하므로, 생성부터 같은 스레드에서 하는 편이 "어느 스레드가
 * correct thread 인가"를 이 클래스 하나로 완결짓는다.
 *
 * ⚠️ **UI 스레드에서 부르면 그만큼 프레임이 멈춘다.** 위 동기 대기 때문에 [encodeFrame]·
 * [finish] 를 `Choreographer` 콜백(예: `withFrameNanos`) 안에서 부르면 전용 스레드가 그
 * 작업을 끝낼 때까지 다음 프레임을 못 그린다. 특히 [finish] 는 코덱이 남은 프레임을 모두 내놓을
 * 때까지 기다리는 [drainCodec] 을 타므로 더 오래 걸릴 수 있어 [EOS_DRAIN_TIMEOUT_MILLIS] 로
 * 상한을 둔다.
 */
class Mp4VideoEncoder(
    private val outputFile: File,
    width: Int,
    height: Int,
    private val framesPerSecond: Int,
) : AutoCloseable {
    // 단일 스레드 executor + submit().get() 을 골랐다: "제출한 작업이 끝날 때까지 호출자를
    // 동기적으로 막는다"는 요구를 가장 적은 개념으로 표현하고, 이 클래스가 코루틴에 기대지 않는
    // 순수 플랫폼 API 래퍼라는 성격과도 맞는다. draw 마다 eglMakeCurrent 로 컨텍스트를 옮겨 타는
    // 대안은, 아직 다른 스레드에 current 로 남아 있으면 EGL_BAD_ACCESS 로 실패해 이 문제를 더
    // 약하게 해결할 뿐이라 쓰지 않았다(BitmapSurfaceWriter KDoc 참고).
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "Mp4VideoEncoder")
    }

    private lateinit var codec: MediaCodec
    private lateinit var muxer: MediaMuxer
    private lateinit var writer: BitmapSurfaceWriter
    private val bufferInfo = MediaCodec.BufferInfo()

    private var trackIndex = -1
    private var isMuxerStarted = false
    private var frameIndex = 0

    // finish()·close() 는 인코더를 만든 스레드가 아닌 임의의 호출부 스레드에서 불릴 수 있고,
    // encodeFrame()·close() 의 가드는 또 다른 호출부 스레드에서 이 값을 읽는다. 두 스레드
    // 사이에는 happens-before 관계가 없어(둘 다 "호출부 스레드"일 뿐 서로를 통해 동기화되지
    // 않는다) 일반 var 로는 한쪽이 쓴 값이 다른 쪽에 보이지 않을 수 있다. @Volatile 로 항상
    // 최신 값을 보게 한다
    @Volatile
    private var isFinished = false

    @Volatile
    private var isClosed = false

    init {
        try {
            runOnEncoderThread {
                val format = MediaFormat.createVideoFormat(MIME_TYPE, width, height).apply {
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                    setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                    setInteger(MediaFormat.KEY_FRAME_RATE, framesPerSecond)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL_SECONDS)
                }
                codec = MediaCodec.createEncoderByType(MIME_TYPE)
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

                writer = BitmapSurfaceWriter(codec.createInputSurface())
                codec.start()

                muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            }
        } catch (e: Throwable) {
            // init 이 중간에 실패하면 생성자가 인스턴스를 반환하지 않아 use{}·close() 경로를 아예
            // 탈 수 없다. 그런데 실패 지점에 따라 코덱·EGL 컨텍스트·전용 스레드가 이미 만들어져
            // 있을 수 있어, 여기서 직접 회수하지 않으면 녹화 재시도가 반복될 때마다 기기 전역
            // 하드웨어 인코더 풀이 고갈된다
            releasePartiallyCreatedResources()
            throw e
        }
    }

    /** [bitmap] 을 다음 프레임으로 넣는다. 호출 순서가 곧 재생 순서다 */
    fun encodeFrame(bitmap: Bitmap) {
        check(!isFinished) { "이미 마감한 인코더에 프레임을 넣었다" }

        runOnEncoderThread {
            val presentationTimeNanos = frameIndex.toLong() * 1_000_000_000L / framesPerSecond
            writer.draw(bitmap = bitmap, presentationTimeNanos = presentationTimeNanos)
            frameIndex++

            drainCodec(endOfStream = false)
        }
    }

    /**
     * 코덱에 EOS 를 신호하고 남은 출력을 모두 muxer 로 옮긴다.
     *
     * **이 호출만으로는 재생 가능한 파일이 되지 않는다.** `muxer.stop()` 은 [close] 안에서
     * 불린다 — 그게 불려야 moov 박스가 파일에 쓰여 재생기가 읽을 수 있다. 그래서 산출물을
     * 실제로 읽는 코드(갤러리 업로드 등)는 이 인스턴스의 `use` 블록 **밖에서**, [close] 가
     * 끝난 뒤에 실행해야 한다. `use { encoder -> ...; encoder.finish() }` 처럼 블록 **안에서**
     * 파일을 열면 헤더가 없는 mp4 를 읽게 된다.
     */
    fun finish() {
        check(!isFinished) { "인코더를 두 번 마감했다" }

        runOnEncoderThread {
            codec.signalEndOfInputStream()
            drainCodec(endOfStream = true)
        }
        isFinished = true
    }

    override fun close() {
        if (isClosed) return
        isClosed = true

        runOnEncoderThread {
            runCatching { writer.close() }
            runCatching {
                codec.stop()
                codec.release()
            }
            runCatching {
                if (isMuxerStarted) muxer.stop()
                muxer.release()
            }

            if (!isFinished && outputFile.exists()) {
                // 헤더가 덜 쓰인 mp4 는 재생되지 않는다. 호출부가 그것을 갤러리에 올리지 못하게 지운다
                val deleted = outputFile.delete()
                if (!deleted) {
                    coreUtilAndroidLogger.w { "중단한 mp4 를 지우지 못했다 - path: ${outputFile.absolutePath}" }
                }
            }
        }

        // 정리 작업은 이미 위에서 동기적으로 끝났다. shutdown() 은 새 작업 제출을 막고 유휴
        // 상태인 전용 스레드를 곧바로 회수한다 — 녹화가 끝난 뒤에도 스레드가 남아 있으면 안 된다
        executor.shutdown()
        runCatching { executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    /**
     * 코덱이 내놓은 출력을 muxer 로 옮긴다.
     *
     * @param endOfStream 참이면 EOS 플래그를 받을 때까지 기다린다. 거짓이면 지금 나온 것만 가져간다 —
     *   프레임마다 기다리면 인코더 파이프라인이 직렬화돼 느려진다
     *
     * @throws IllegalStateException [endOfStream] 이 참인데 [EOS_DRAIN_TIMEOUT_MILLIS] 안에
     *   코덱이 EOS 를 내놓지 않으면 던진다. 이 메서드는 호출부 스레드를 막고 있으므로(클래스
     *   KDoc 참고), 상한이 없으면 `while(true)` 가 그 스레드를 영원히 멈춰 세운다 — UI 스레드에서
     *   부른 경우 실패로 드러나는 대신 ANR 이 된다
     */
    private fun drainCodec(endOfStream: Boolean) {
        // endOfStream 이 거짓이면 INFO_TRY_AGAIN_LATER 에서 즉시 return 하므로 무한 루프가 될 수
        // 없다 — 상한은 endOfStream 이 참인 경로에만 필요하다
        val eosDeadlineNanos = if (endOfStream) {
            System.nanoTime() + EOS_DRAIN_TIMEOUT_MILLIS * NANOS_PER_MILLI
        } else {
            null
        }

        while (true) {
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DEQUEUE_TIMEOUT_MICROS)

            when {
                outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    if (!endOfStream) return
                    check(System.nanoTime() < checkNotNull(eosDeadlineNanos)) {
                        "코덱이 ${EOS_DRAIN_TIMEOUT_MILLIS}ms 안에 EOS 를 내놓지 않았다"
                    }
                }

                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    check(!isMuxerStarted) { "출력 포맷이 두 번 바뀌었다" }
                    trackIndex = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                    isMuxerStarted = true
                }

                outputIndex >= 0 -> {
                    val buffer = requireNotNull(codec.getOutputBuffer(outputIndex))

                    // CODEC_CONFIG 은 addTrack 이 이미 포맷으로 받아 간 값이라 트랙에 쓰면 중복이다
                    val isCodecConfig = bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                    if (!isCodecConfig && bufferInfo.size > 0) {
                        check(isMuxerStarted) { "트랙을 열기 전에 샘플이 나왔다" }
                        buffer.position(bufferInfo.offset)
                        buffer.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, buffer, bufferInfo)
                    }

                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                }
            }
        }
    }

    /**
     * [init] 이 중간에 실패했을 때 이미 만들어졌을 수 있는 리소스를 회수한다.
     *
     * 어디까지 만들어졌는지 모르므로 `lateinit` 프로퍼티마다 초기화 여부를 확인하고, 리소스별로
     * 개별 [runCatching] 으로 감싼다 — 하나가 던져도 나머지 회수를 막으면 안 된다. 리소스를
     * 만든(=EGL 컨텍스트가 묶인) 스레드에서 그대로 정리해야 하므로 아직 shutdown 하지 않은
     * `executor` 에 한 번 더 제출해서 실행하고, 끝나면 `executor` 자체를 폐기한다. 이미 실패한
     * 상황이라 새 작업이 큐에 남아 있을 수 없어 `shutdown()` 대신 즉시 회수하는 `shutdownNow()`
     * 를 쓴다
     */
    private fun releasePartiallyCreatedResources() {
        runCatching {
            executor
                .submit {
                    if (::writer.isInitialized) runCatching { writer.close() }
                    if (::codec.isInitialized) {
                        runCatching { codec.stop() }
                        runCatching { codec.release() }
                    }
                    if (::muxer.isInitialized) runCatching { muxer.release() }
                }.get()
        }
        executor.shutdownNow()
    }

    /**
     * [block] 을 전용 스레드에 제출하고 끝날 때까지 호출한 스레드를 막는다.
     *
     * `Future.get()` 은 작업 중 던진 예외를 [ExecutionException] 으로 감싼다 — 그대로 두면
     * `check()` 실패 같은 내부 불변식 위반이 호출부에는 엉뚱한 예외 타입으로 보인다. 원래
     * 예외를 풀어서 다시 던져, 이 메서드가 마치 같은 스레드에서 [block] 을 직접 부른 것과
     * 같은 예외 투명성을 갖게 한다.
     */
    private fun runOnEncoderThread(block: () -> Unit) {
        try {
            executor.submit(block).get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    private companion object {
        const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        const val BIT_RATE = 6_000_000
        const val I_FRAME_INTERVAL_SECONDS = 1
        const val DEQUEUE_TIMEOUT_MICROS = 10_000L

        /** [close] 가 스레드를 정리할 때 기다리는 최대 시간. 이미 마지막 작업까지 끝낸 뒤라 사실상 즉시 끝난다 */
        const val EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 1L

        /**
         * [drainCodec] 이 EOS 를 기다리며 총 대기할 수 있는 상한.
         *
         * [finish] 는 이 안에서 호출부 스레드를 동기적으로 막는다(클래스 KDoc 참고) — 상한이 없으면
         * 코덱이 EOS 를 끝내 내놓지 않을 때 `while(true)` 로 영원히 멈춘다. 프레임 루프가 UI 스레드
         * (`AndroidUiDispatcher.Main`)에서 돌기 때문에 그 경우 실패 토스트가 아니라 ANR 로 이어진다.
         * 정상적인 코덱은 EOS 신호 뒤 수십 ms 안에 응답하므로 3초는 이미 넉넉한 여유이면서, 안드로이드가
         * 입력 이벤트를 ANR 로 판정하는 5초보다는 짧게 잡아 "무한정 멈춤" 대신 "예외로 드러나는 실패"가
         * 되도록 한다
         */
        const val EOS_DRAIN_TIMEOUT_MILLIS = 3_000L

        const val NANOS_PER_MILLI = 1_000_000L
    }
}
