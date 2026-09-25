package com.teamyg.parfait.data.model.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UploadImageFormatTest {
    @Test
    fun ofExtension_uppercase_resolves() {
        // 갤러리·카메라가 대문자 확장자를 주는 기기가 있다
        assertEquals(UploadImageFormat.PNG, UploadImageFormat.ofExtension("PNG"))
    }

    @Test
    fun contentType_jpeg_isNotImageJpg() {
        // 서버는 image/jpg 를 INVALID_CONTENT_TYPE 으로 거절한다
        assertEquals("image/jpeg", UploadImageFormat.JPEG.contentType)
        assertEquals("image/png", UploadImageFormat.PNG.contentType)
    }

    @Test
    fun ofBytes_readsTheMagicNumber() {
        // Given 확장자와 무관하게 실제 내용이 PNG·JPEG 인 바이트
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(16)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(16)

        // Then 이름이 거짓이어도 내용으로 판정한다
        assertEquals(UploadImageFormat.PNG, UploadImageFormat.ofBytes(png))
        assertEquals(UploadImageFormat.JPEG, UploadImageFormat.ofBytes(jpeg))
    }

    @Test
    fun ofBytes_neitherFormat_isNull() {
        assertNull(UploadImageFormat.ofBytes(byteArrayOf(0x47, 0x49, 0x46, 0x38)))
        assertNull(UploadImageFormat.ofBytes(byteArrayOf()))
    }
}
