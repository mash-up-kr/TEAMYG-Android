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
import org.junit.Assert.assertThrows
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

        // duration 은 마지막 PTS 로 정해져 중간 유실을 못 잡는다. 샘플 수를 따로 센다
        assertEquals(framesPerSecond, countVideoSamples(output))

        output.delete()
        bitmap.recycle()
    }

    @Test
    fun encodeFrame_calledFromDifferentThreadThanConstructor_stillEncodesAllFrames() {
        // Given 인코더를 만드는 스레드와 encodeFrame·finish 를 부르는 스레드를 서로 다르게 둔다.
        // 전용 스레드가 없으면 EGL 이 생성 스레드에 묶여 draw 가 조용히 실패한다
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

    // Bitmap.createBitmap(Picture) 가 API 28+ 이고, HARDWARE 경로도 API 28+ 에서만 생긴다
    @SdkSuppress(minSdkVersion = 28)
    @Test
    fun encodeFrame_hardwareConfigBitmap_stillEncodesAllFrames() {
        // Given GraphicsLayer.toImageBitmap() 이 실제로 내놓는 것과 같은 Config.HARDWARE 비트맵
        val output = File(context.cacheDir, "encoder_hardware_${System.nanoTime()}.mp4")
        val picture = Picture()
        val pictureCanvas = picture.beginRecording(width, height)
        pictureCanvas.drawColor(Color.rgb(0, 0, 255))
        picture.endRecording()
        val bitmap = Bitmap.createBitmap(picture)

        // 전제: 정말 HARDWARE 여야 이 경로를 탄다
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

    @Test
    fun encodeFrame_afterClose_throwsIllegalState() {
        // Given 닫힌 인코더
        val output = File(context.cacheDir, "encoder_closed_${System.nanoTime()}.mp4")
        val bitmap = createBitmap(width, height)
        val encoder = Mp4VideoEncoder(
            outputFile = output,
            width = width,
            height = height,
            framesPerSecond = framesPerSecond,
        )
        encoder.close()

        // When · Then 프레임을 넣으면 원인이 드러나는 예외를 던진다
        assertThrows(IllegalStateException::class.java) { encoder.encodeFrame(bitmap) }

        output.delete()
        bitmap.recycle()
    }

    /** [file] 의 첫 비디오 트랙에 쓰인 샘플(=인코딩된 프레임) 수 */
    private fun countVideoSamples(file: File): Int {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)
        val videoTrackIndex = (0 until extractor.trackCount).first { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
        }
        extractor.selectTrack(videoTrackIndex)

        var sampleCount = 0
        val sampleBuffer = ByteBuffer.allocate(width * height)
        while (extractor.readSampleData(sampleBuffer, 0) >= 0) {
            sampleCount++
            extractor.advance()
        }
        extractor.release()

        return sampleCount
    }
}
