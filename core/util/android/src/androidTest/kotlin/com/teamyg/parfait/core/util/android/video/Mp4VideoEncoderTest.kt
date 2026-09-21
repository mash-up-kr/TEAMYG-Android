package com.teamyg.parfait.core.util.android.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Picture
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@RunWith(AndroidJUnit4::class)
@LargeTest
class Mp4VideoEncoderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val width = 720
    private val height = 1280
    private val framesPerSecond = 30

    @Test
    fun finish_thirtyFrames_writesOneSecondPlayableMp4() {
        // Given 출력 파일과 한 색으로 채운 프레임
        val output = File(context.cacheDir, "encoder_test_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 0, 255)) }

        // When 30프레임을 인코딩한다
        Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        ).use { encoder ->
            repeat(framesPerSecond) { encoder.encodeFrame(bitmap) }
            encoder.finish()
        }

        // Then 열리는 mp4 가 나오고 길이가 1초에 가깝다
        assertTrue(output.exists())
        assertTrue(output.length() > 0L)

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(output.absolutePath)
        val durationMs = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLong()
        val videoWidth = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            ?.toInt()
        val videoHeight = retriever
            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            ?.toInt()
        retriever.release()

        assertEquals(width, videoWidth)
        assertEquals(height, videoHeight)
        assertTrue("길이가 $durationMs ms 다", durationMs != null && durationMs in 800L..1400L)

        // duration 은 사실상 마지막 프레임의 PTS 로 정해진다 — 중간 프레임이 유실돼도 마지막
        // 프레임 타임스탬프만 맞으면 duration 검증은 그대로 통과해 버린다. 그래서 duration 과는
        // 별개로 컨테이너에 실제로 쓰인 샘플(=인코딩된 프레임) 개수를 MediaExtractor 로 세어
        // 정확히 framesPerSecond 개인지 단언한다. 이게 "프레임이 유실되지 않는다"를 코드로 잠근다
        assertEquals(framesPerSecond, countVideoSamples(output))

        output.delete()
        bitmap.recycle()
    }

    @Test
    fun encodeFrame_calledFromDifferentThreadThanConstructor_stillEncodesAllFrames() {
        // Given 인코더를 만드는 스레드와 encodeFrame·finish 를 부르는 스레드를 서로 다르게 둔다.
        // BitmapSurfaceWriter 의 EGL 컨텍스트는 그것을 생성한 스레드에 묶이므로(그 클래스 KDoc
        // 참고), Mp4VideoEncoder 가 내부적으로 전용 단일 스레드에 marshalling 하지 않으면
        // encodeFrame 의 draw 호출이 예외 없이 조용히 실패해 프레임이 유실된다. 이 테스트가
        // 통과해야 그 전용 스레드 설계가 실제로 살아 있다는 증거가 된다 — 기존 두 테스트는 생성자와
        // encodeFrame·finish·close 를 전부 같은 계측 스레드에서 불러서, 전용 스레드를 통째로
        // 제거해도 통과해 버려 이 설계를 전혀 검증하지 못했다
        val output = File(context.cacheDir, "encoder_cross_thread_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 0, 255)) }

        val constructorThread = Executors.newSingleThreadExecutor()
        val callerThread = Executors.newSingleThreadExecutor()
        try {
            // When 생성은 constructorThread 에서, encodeFrame·finish·close 는 callerThread 에서 부른다
            val encoder = constructorThread
                .submit<Mp4VideoEncoder> {
                    Mp4VideoEncoder(
                        outputFile = output,
                        width = width,
                        height = height,
                        framesPerSecond = framesPerSecond,
                    )
                }.get()

            callerThread
                .submit {
                    repeat(framesPerSecond) { encoder.encodeFrame(bitmap) }
                    encoder.finish()
                    encoder.close()
                }.get()
        } finally {
            constructorThread.shutdown()
            callerThread.shutdown()
        }

        // Then 서로 다른 두 스레드에서 호출했음에도 30 프레임이 그대로 인코딩된다
        assertEquals(framesPerSecond, countVideoSamples(output))

        output.delete()
        bitmap.recycle()
    }

    // Bitmap.createBitmap(Picture) 는 API 28(P)부터 있다 — minSdk 26 인 이 앱이 API 26·27
    // 기기에서 이 테스트를 그대로 돌리면 NoSuchMethodError 로 죽는다. 이 테스트가 검증하는
    // HARDWARE 비트맵 경로 자체가 API 28+ 에서만 실제로 벌어지므로(BitmapSurfaceWriter.draw
    // KDoc 참고) 그 아래 API 는 애초에 이 테스트의 대상이 아니다
    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun encodeFrame_hardwareConfigBitmap_stillEncodesAllFrames() {
        // Given Compose 의 GraphicsLayer.toImageBitmap() 이 API 28+ 에서 실제로 내놓는 것과 같은
        // Config.HARDWARE 비트맵. createBitmap(w, h) 로 만드는 소프트웨어 ARGB_8888 비트맵으로는
        // 이 경로가 한 번도 실행되지 않는다 — 인코더가 실제로 받는 것은 HARDWARE 비트맵이라, 그것을
        // Picture 에 그린 뒤 Bitmap.createBitmap(Picture) 로 만들어야 API 28+ 에서 재현된다
        val output = File(context.cacheDir, "encoder_hardware_${System.nanoTime()}.mp4")
        val picture = Picture()
        val pictureCanvas = picture.beginRecording(width, height)
        pictureCanvas.drawColor(Color.rgb(0, 0, 255))
        picture.endRecording()
        val bitmap = Bitmap.createBitmap(picture)

        // 이 단언이 실패하면 테스트 환경이 HARDWARE 비트맵을 애초에 못 만든 것이라, 아래 인코딩
        // 결과가 통과해도 이 테스트가 검증하려는 경로를 타지 않은 것이다 — 먼저 전제부터 못박는다
        assertEquals(Bitmap.Config.HARDWARE, bitmap.config)

        // When 그 하드웨어 비트맵을 그대로 인코딩한다
        Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        ).use { encoder ->
            repeat(framesPerSecond) { encoder.encodeFrame(bitmap) }
            encoder.finish()
        }

        // Then 소프트웨어 비트맵과 마찬가지로 프레임 유실 없이 인코딩된다
        assertEquals(framesPerSecond, countVideoSamples(output))

        output.delete()
    }

    @Test
    fun close_withoutFinish_leavesNoPartialFileBehind() {
        // Given 출력 파일
        val output = File(context.cacheDir, "encoder_abort_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 0, 255)) }

        // When finish 없이 닫는다 (인코딩 중 실패 상황)
        Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        ).use { encoder ->
            encoder.encodeFrame(bitmap)
        }

        // Then 재생할 수 없는 파일을 남기지 않는다
        assertTrue("중단한 산출물이 남았다", !output.exists() || output.length() == 0L)

        output.delete()
        bitmap.recycle()
    }

    /**
     * [file] 의 첫 비디오 트랙에 실제로 쓰인 샘플(=인코딩된 프레임) 개수를 센다.
     *
     * `MediaMetadataRetriever` 의 duration 은 마지막 프레임의 PTS 에 좌우돼 중간 프레임 유실을
     * 잡아내지 못한다(각 테스트의 주석 참고) — 컨테이너를 직접 읽어 샘플 개수를 세야 유실 여부가
     * 드러난다
     */
    private fun countVideoSamples(file: File): Int {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        val videoTrackIndex = (0 until extractor.trackCount).first { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        }
        extractor.selectTrack(videoTrackIndex)

        var sampleCount = 0
        // 압축된 샘플이라 원본 픽셀 수보다 훨씬 작다. 프레임 하나가 다 안 들어갈 정도로 작지만
        // 않으면 되므로 넉넉히 잡는다
        val sampleBuffer = ByteBuffer.allocate(width * height)
        while (extractor.readSampleData(sampleBuffer, 0) >= 0) {
            sampleCount++
            extractor.advance()
        }
        extractor.release()

        return sampleCount
    }
}
