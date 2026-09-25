package com.teamyg.parfait.data.model.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UploadImageFormatTest {
    @Test
    fun ofExtension_jpgAndJpeg_bothResolveToJpeg() {
        // Given 같은 형식을 가리키는 두 표기
        // Then 어느 쪽으로 적힌 파일이든 받는다
        assertEquals(UploadImageFormat.JPEG, UploadImageFormat.ofExtension("jpg"))
        assertEquals(UploadImageFormat.JPEG, UploadImageFormat.ofExtension("jpeg"))
    }

    @Test
    fun ofExtension_uppercase_resolves() {
        // 갤러리·카메라가 대문자 확장자를 주는 기기가 있다
        assertEquals(UploadImageFormat.PNG, UploadImageFormat.ofExtension("PNG"))
    }

    @Test
    fun ofExtension_unsupported_isNull() {
        // Then 임의로 하나를 고르지 않는다 — 거짓 contentType 은 S3 를 통과해 버린다
        assertNull(UploadImageFormat.ofExtension("webp"))
        assertNull(UploadImageFormat.ofExtension(""))
    }

    @Test
    fun contentType_jpeg_isNotImageJpg() {
        // 서버는 image/jpg 를 INVALID_CONTENT_TYPE 으로 거절한다
        assertEquals("image/jpeg", UploadImageFormat.JPEG.contentType)
        assertEquals("image/png", UploadImageFormat.PNG.contentType)
    }

    @Test
    fun extension_jpeg_isTheCanonicalOne() {
        // Given 표기가 둘인 형식
        // Then 파일명을 지을 때는 하나로 고정한다
        assertEquals("jpg", UploadImageFormat.JPEG.extension)
        assertEquals("png", UploadImageFormat.PNG.extension)
    }

    @Test
    fun ofContentType_resolvesBothWays() {
        assertEquals(UploadImageFormat.PNG, UploadImageFormat.ofContentType("image/png"))
        assertEquals(UploadImageFormat.JPEG, UploadImageFormat.ofContentType("image/jpeg"))
    }

    @Test
    fun ofContentType_nullOrUnknown_isNull() {
        // `file://` 은 시스템이 MIME 을 모르는 일이 흔해 null 이 그대로 넘어온다
        assertNull(UploadImageFormat.ofContentType(null))
        assertNull(UploadImageFormat.ofContentType("image/jpg"))
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
