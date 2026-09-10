package com.teamyg.parfait.feature.groups.canvas.impl.util

import android.graphics.Bitmap
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class CanvasCaptureHolderTest {
    // 홀더가 object 라 상태가 테스트 클래스 경계도 넘는다. 앞뒤로 다 비운다
    @BeforeTest
    fun emptyBefore() = CanvasCaptureHolder.clear()

    @AfterTest
    fun emptyAfter() = CanvasCaptureHolder.clear()

    @Test
    fun peek_afterPut_givesSameBitmap() {
        val bitmap = mockk<Bitmap>()

        CanvasCaptureHolder.put(bitmap)

        assertSame(bitmap, CanvasCaptureHolder.peek())
    }

    @Test
    fun peek_calledTwice_stillGivesSameBitmap() {
        // Given 미리보기가 다시 컴포즈되면 같은 자리를 한 번 더 읽는다
        val bitmap = mockk<Bitmap>()
        CanvasCaptureHolder.put(bitmap)

        CanvasCaptureHolder.peek()

        assertSame(bitmap, CanvasCaptureHolder.peek())
    }

    @Test
    fun put_overExistingCapture_keepsOnlyTheNewOne() {
        CanvasCaptureHolder.put(mockk<Bitmap>())
        val newer = mockk<Bitmap>()

        CanvasCaptureHolder.put(newer)

        assertSame(newer, CanvasCaptureHolder.peek())
    }

    @Test
    fun peek_whenEmpty_givesNull() {
        assertNull(CanvasCaptureHolder.peek())
    }
}
