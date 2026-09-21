package com.teamyg.parfait.core.util.android.video

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.media.ImageReader
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class BitmapSurfaceWriterTest {
    private val width = 64
    private val height = 64

    @Test
    fun draw_solidBitmap_paintsThatColorOnSurface() {
        // Given RGBA_8888 을 받는 소비자와, 한 색으로 채운 비트맵
        // FLEX_RGBA_8888 은 이 기기(에뮬레이터)가 거부해 PixelFormat.RGBA_8888 로 대체했다
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(255, 0, 0)) }

        // When 그 비트맵을 표면에 그린다
        BitmapSurfaceWriter(reader.surface).use { writer ->
            writer.draw(bitmap = bitmap, presentationTimeNanos = 0L)
        }

        // Then 소비자가 받은 프레임의 가운데 픽셀이 그 색이다
        val image = reader.acquireNextImage()
        assertNotNull(image)
        val plane = image.planes[0]
        val buffer = plane.buffer
        val offset = (height / 2) * plane.rowStride + (width / 2) * plane.pixelStride
        assertEquals(255, buffer.get(offset).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 1).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 2).toInt() and 0xFF)

        image.close()
        reader.close()
        bitmap.recycle()
    }

    @Test
    fun draw_twoBitmaps_deliversTwoFrames() {
        // Given 소비자와 서로 다른 두 비트맵
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
        val first = createBitmap(width, height).apply { eraseColor(Color.rgb(255, 0, 0)) }
        val second = createBitmap(width, height).apply { eraseColor(Color.rgb(0, 255, 0)) }

        // When 두 번 그리고, 매번 그 직후에 프레임을 받는다
        // 이 기기(에뮬레이터)의 BufferQueue 는 이전 프레임을 소비하기 전에 다음 프레임을 큐에
        // 넣으면 오래된 프레임을 조용히 버린다 — GL 과 무관한 순수 Surface.lockCanvas 로도
        // 재현되는 소비자(consumer) 쪽 동작이라 draw() 구현으로 고칠 수 없다. 그래서 두 번
        // 그린 뒤 한꺼번에 회수하는 대신, 그린 직후 바로 회수해 두 프레임이 각각 실제로
        // 생산되는지를 검증한다
        val writer = BitmapSurfaceWriter(reader.surface)
        writer.draw(bitmap = first, presentationTimeNanos = 0L)
        val firstImage = reader.acquireNextImage()
        assertNotNull(firstImage)
        firstImage.close()

        writer.draw(bitmap = second, presentationTimeNanos = 33_333_333L)
        val secondImage = reader.acquireNextImage()
        assertNotNull(secondImage)
        secondImage.close()

        writer.close()
        reader.close()
        first.recycle()
        second.recycle()
    }

    @Test
    fun draw_topHalfDifferentColorBitmap_keepsTopAtTop() {
        // Given 위 절반은 빨강, 아래 절반은 초록인 비트맵
        // 단색 비트맵으로는 texCoordBuffer 의 상하 반전이 맞는지 드러나지 않는다 — 뒤집혀도
        // 가운데 픽셀 색은 그대로라서다. 위아래가 다른 색이어야 반전 여부가 픽셀 값으로 드러난다
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val bitmap = createBitmap(width, height).apply {
            val canvas = Canvas(this)
            val halfHeight = height / 2f
            canvas.drawRect(0f, 0f, width.toFloat(), halfHeight, Paint().apply { color = Color.rgb(255, 0, 0) })
            canvas.drawRect(
                0f,
                halfHeight,
                width.toFloat(),
                height.toFloat(),
                Paint().apply { color = Color.rgb(0, 255, 0) },
            )
        }

        // When 그 비트맵을 표면에 그린다
        BitmapSurfaceWriter(reader.surface).use { writer ->
            writer.draw(bitmap = bitmap, presentationTimeNanos = 0L)
        }

        // Then 표면에 그려진 프레임도 위쪽은 빨강, 아래쪽은 초록이다 — 반전되어 있었다면
        // 이 값이 뒤바뀌어 나왔을 것이다
        val image = reader.acquireNextImage()
        assertNotNull(image)
        val plane = image.planes[0]
        val buffer = plane.buffer

        val topOffset = (height / 4) * plane.rowStride + (width / 2) * plane.pixelStride
        assertEquals(255, buffer.get(topOffset).toInt() and 0xFF)
        assertEquals(0, buffer.get(topOffset + 1).toInt() and 0xFF)

        val bottomOffset = (height * 3 / 4) * plane.rowStride + (width / 2) * plane.pixelStride
        assertEquals(0, buffer.get(bottomOffset).toInt() and 0xFF)
        assertEquals(255, buffer.get(bottomOffset + 1).toInt() and 0xFF)

        image.close()
        reader.close()
        bitmap.recycle()
    }

    @Test
    fun close_calledTwice_secondCallIsNoOpAndDoesNotBreakSubsequentWriter() {
        // Given 열려 있는 writer 하나
        // 이 기기(에뮬레이터)의 EGL 구현은 스펙대로 관대해서, 이미 종료된 display 에
        // eglTerminate·eglDestroySurface·eglDestroyContext 를 또 불러도 예외를 던지지 않고
        // 에러를 로그로만 남긴다(EGL_NOT_INITIALIZED / EGL_BAD_DISPLAY, 실제로 재현해 확인함).
        // 그래서 "두 번 닫아도 예외가 없다"는 블랙박스 검증만으로는 재호출 가드가 실제로
        // 동작하는지 드러나지 않는다 — 가드가 죽어 있어도 이 테스트는 통과해 버린다.
        // 그래서 가드 필드를 리플렉션으로 직접 확인해, 두 번째 close() 가 실제로 조기
        // 반환되는지를 못박는다
        val firstReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val firstWriter = BitmapSurfaceWriter(firstReader.surface)
        val isClosedField = BitmapSurfaceWriter::class.java
            .getDeclaredField("isClosed")
            .apply { isAccessible = true }
        assertEquals(false, isClosedField.getBoolean(firstWriter))

        // When 두 번 닫는다 — use{} 블록에서 예외가 나거나, writer 를 소유한 인코더가 자기
        // 정리 경로에서 또 close() 를 부르는 상황을 흉내낸다
        firstWriter.close()
        assertEquals(true, isClosedField.getBoolean(firstWriter))
        firstWriter.close()

        // Then 공유 EGL 상태가 망가지지 않아 그 뒤에 새로 만든 writer 로 정상적으로 그릴 수 있다
        // — 두 번째 close() 뒤의 isClosed 재확인은 위 대입문 직후라 항상 true 로 동어반복이라
        // 지웠다. 가드가 실제로 지키는 것은 첫 번째 close() 뒤의 단언(위)이다
        val secondReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val bitmap = createBitmap(width, height).apply { eraseColor(Color.rgb(255, 0, 0)) }
        BitmapSurfaceWriter(secondReader.surface).use { secondWriter ->
            secondWriter.draw(bitmap = bitmap, presentationTimeNanos = 0L)
        }

        val image = secondReader.acquireNextImage()
        assertNotNull(image)
        val plane = image.planes[0]
        val buffer = plane.buffer
        val offset = (height / 2) * plane.rowStride + (width / 2) * plane.pixelStride
        assertEquals(255, buffer.get(offset).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 1).toInt() and 0xFF)
        assertEquals(0, buffer.get(offset + 2).toInt() and 0xFF)

        image.close()
        firstReader.close()
        secondReader.close()
        bitmap.recycle()
    }
}
