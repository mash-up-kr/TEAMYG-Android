package com.teamyg.parfait.core.util.android.extension

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
@SmallTest
class ContentResolverDecodeTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val file = File(context.cacheDir, "content-resolver-decode-test.jpg")

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun decodeUriToBitmap_longSideOverLimit_isDecodedAtCappedSize() {
        // Given 긴 변이 상한의 두 배인 띠. 치수만 넘기면 되므로 픽셀 수는 작게 잡는다
        writeJpeg(width = DECODE_LONG_SIDE_LIMIT * 2, height = 64)

        // When
        val decoded = context.contentResolver.decodeUriToBitmap(file.toUri())

        // Then
        assertEquals(DECODE_LONG_SIDE_LIMIT, decoded.width)
        assertEquals(32, decoded.height)
    }

    @Test
    fun decodeUriToBitmap_withinLimit_keepsSourceSize() {
        // Given
        writeJpeg(width = 1000, height = 500)

        // When
        val decoded = context.contentResolver.decodeUriToBitmap(file.toUri())

        // Then
        assertEquals(1000, decoded.width)
        assertEquals(500, decoded.height)
    }

    private fun writeJpeg(
        width: Int,
        height: Int,
    ) {
        val bitmap = createBitmap(width, height)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
    }
}
