package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.content.Context
import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertTrue

class CanvasCaptureCacheTest {
    @get:Rule
    val cacheDir = TemporaryFolder()

    private fun context(): Context = mockk<Context>().also {
        every { it.cacheDir } returns cacheDir.root
    }

    @Test
    fun write_whenCompressFails_isFailure() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.compress(any(), any(), any()) } returns false

        assertTrue(bitmap.writeToCanvasCaptureCache(context()).isFailure)
    }

    @Test
    fun write_whenCompressSucceeds_isSuccess() {
        val bitmap = mockk<Bitmap>()
        every { bitmap.compress(any(), any(), any()) } returns true

        assertTrue(bitmap.writeToCanvasCaptureCache(context()).isSuccess)
    }
}
