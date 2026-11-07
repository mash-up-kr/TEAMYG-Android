package com.teamyg.parfait.feature.camera.impl.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.unit.IntRect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CroppedImageSaveTest {
    private lateinit var source: Bitmap
    private lateinit var jpeg: ByteArray
    private lateinit var file: File

    @Before
    fun setUp() {
        source = halfRedHalfBlueBitmap()
        jpeg = ByteArrayOutputStream().use { output ->
            source.compress(Bitmap.CompressFormat.JPEG, 100, output)
            output.toByteArray()
        }
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        file = File.createTempFile("cropped", ".jpg", cacheDir)
    }

    @After
    fun tearDown() {
        file.delete()
        source.recycle()
    }

    @Test
    fun saveCroppedJpeg_rightHalfRotated90_isBluePortrait() {
        saveCroppedJpeg(jpeg = jpeg, cropRect = IntRect(200, 0, 400, 300), rotationDegrees = 90, file = file)

        val saved = decodeSaved()
        assertEquals(300, saved.width)
        assertEquals(200, saved.height)
        assertTrue(saved.isBlueAt(saved.width / 2, saved.height / 2))
    }

    @Test
    fun saveCroppedJpeg_noCropRect_keepsWholeFrame() {
        saveCroppedJpeg(jpeg = jpeg, cropRect = null, rotationDegrees = 0, file = file)

        val saved = decodeSaved()
        assertEquals(400, saved.width)
        assertEquals(300, saved.height)
    }

    @Test
    fun saveCroppedCapture_leftHalf_isRed() {
        saveCroppedCapture(captured = source, cropRect = IntRect(0, 0, 200, 300), rotationDegrees = 0, file = file)

        val saved = decodeSaved()
        assertEquals(200, saved.width)
        assertEquals(300, saved.height)
        assertTrue(saved.isRedAt(saved.width / 2, saved.height / 2))
    }

    private fun decodeSaved(): Bitmap = checkNotNull(BitmapFactory.decodeFile(file.absolutePath))
}

private fun halfRedHalfBlueBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 300, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawRect(0f, 0f, 200f, 300f, Paint().apply { color = Color.RED })
    canvas.drawRect(200f, 0f, 400f, 300f, Paint().apply { color = Color.BLUE })
    return bitmap
}

// JPEG 손실을 견디도록 정확한 값 대신 채널 우세로 본다
private fun Bitmap.isBlueAt(
    x: Int,
    y: Int,
): Boolean {
    val pixel = getPixel(x, y)
    return Color.blue(pixel) > Color.red(pixel) + COLOR_DOMINANCE_MARGIN
}

private fun Bitmap.isRedAt(
    x: Int,
    y: Int,
): Boolean {
    val pixel = getPixel(x, y)
    return Color.red(pixel) > Color.blue(pixel) + COLOR_DOMINANCE_MARGIN
}

private const val COLOR_DOMINANCE_MARGIN = 50
