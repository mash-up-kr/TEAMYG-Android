package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val PNG_SIGNATURE = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

class ByteArrayExtensionTest {
    @Test
    fun startsWith_sameHead_isTrue() {
        // Given 시그니처 뒤로 내용이 더 붙은 바이트
        val bytes = PNG_SIGNATURE + byteArrayOf(0x0D, 0x0A, 0x1A, 0x0A)

        // Then 뒤가 무엇이든 앞머리만 본다
        assertTrue(bytes.startsWith(PNG_SIGNATURE))
    }

    @Test
    fun startsWith_differentHead_isFalse() {
        val bytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        assertFalse(bytes.startsWith(PNG_SIGNATURE))
    }

    @Test
    fun startsWith_shorterThanPrefix_isFalse() {
        // Given 시그니처보다 짧아 끝까지 비교할 수 없는 바이트
        val bytes = byteArrayOf(0x89.toByte(), 0x50)

        // Then 앞부분이 같아도 참이라고 하지 않는다 — 여기서 참이 되면 잘린 파일이 통과한다
        assertFalse(bytes.startsWith(PNG_SIGNATURE))
    }

    @Test
    fun startsWith_emptyPrefix_isTrue() {
        assertTrue(byteArrayOf(0x01).startsWith(byteArrayOf()))
    }
}
