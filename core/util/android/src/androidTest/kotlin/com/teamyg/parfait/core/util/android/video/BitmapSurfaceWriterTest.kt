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

        // When 두 번 그리고, 매번 그 직후에 프레임을 받는다 — 몰아서 그리면 소비자가 이전 프레임을 버린다
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
        // Given 위 절반은 빨강, 아래 절반은 초록인 비트맵 — 단색으로는 상하 반전이 드러나지 않는다
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

        // Then 표면에 그려진 프레임도 위쪽은 빨강, 아래쪽은 초록이다
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
        // EGL 은 종료된 display 에 다시 불러도 예외 없이 로그만 남겨, 블랙박스로는 가드가 안 보인다.
        // 그래서 가드 필드를 리플렉션으로 확인한다
        val firstReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val firstWriter = BitmapSurfaceWriter(firstReader.surface)
        val isClosedField = BitmapSurfaceWriter::class.java
            .getDeclaredField("isClosed")
            .apply { isAccessible = true }
        assertEquals(false, isClosedField.getBoolean(firstWriter))

        // When 두 번 닫는다
        firstWriter.close()
        assertEquals(true, isClosedField.getBoolean(firstWriter))
        firstWriter.close()

        // Then 공유 EGL 상태가 망가지지 않아 새 writer 로 정상적으로 그릴 수 있다
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
