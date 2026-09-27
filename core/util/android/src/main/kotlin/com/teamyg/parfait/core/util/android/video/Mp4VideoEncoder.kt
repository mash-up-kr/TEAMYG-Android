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
 * 타임스탬프는 벽시계가 아니라 프레임 번호 ÷ [framesPerSecond] 다 — 느린 기기에서도 재생 속도가 같아야 한다.
 * [finish] 없이 닫으면 재생 불가한 산출물을 지운다.
 *
 * **어느 스레드에서 불러도 되지만 호출부를 동기로 막는다.** [BitmapSurfaceWriter] 의 EGL 컨텍스트가
 * 생성 스레드에 묶이므로 생성자부터 [close] 까지 전부 전용 단일 스레드에서 실행한다.
 * UI 스레드에서 부르면 그만큼 프레임이 멈춘다.
 */
class Mp4VideoEncoder(
    private val outputFile: File,
    width: Int,
    height: Int,
    private val framesPerSecond: Int,
) : AutoCloseable {
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

    // 전용 스레드 밖, 서로 다른 호출부 스레드에서 읽고 쓴다
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
            // 인스턴스가 반환되지 않아 close() 를 탈 수 없다. 여기서 회수하지 않으면 재시도마다
            // 기기 전역 하드웨어 인코더가 샌다
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
     * 코덱에 EOS 를 신호하고 남은 출력을 muxer 로 옮긴다.
     *
     * **이것만으로는 재생 가능한 파일이 아니다.** moov 박스는 [close] 의 `muxer.stop()` 이 쓴다.
     * 산출물은 `use` 블록 밖에서 읽어야 한다.
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

        executor.shutdown()
        runCatching { executor.awaitTermination(EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
    }

    /**
     * 코덱 출력을 muxer 로 옮긴다.
     *
     * @param endOfStream 참이면 EOS 까지 기다린다. 거짓이면 지금 나온 것만 가져간다 —
     *   프레임마다 기다리면 파이프라인이 직렬화된다
     */
    private fun drainCodec(endOfStream: Boolean) {
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

    /** [init] 이 중간에 실패했을 때 만들어진 것까지만 회수한다. EGL 이 묶인 전용 스레드에서 정리해야 한다 */
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

    /** [block] 을 전용 스레드에서 돌리고 끝날 때까지 막는다. [ExecutionException] 은 벗겨 원래 예외를 던진다 */
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

        const val EXECUTOR_SHUTDOWN_TIMEOUT_SECONDS = 1L

        /** EOS 대기 상한. 호출부가 UI 스레드일 수 있어, 없으면 코덱이 멈출 때 실패가 아니라 ANR 이 된다. 입력 ANR 5초보다 짧게 잡는다 */
        const val EOS_DRAIN_TIMEOUT_MILLIS = 3_000L

        const val NANOS_PER_MILLI = 1_000_000L
    }
}
