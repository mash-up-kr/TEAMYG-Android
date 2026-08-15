package com.teamyg.parfait.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecureRandomNonceGeneratorTest {
    private val generator = SecureRandomNonceGenerator()

    @Test
    fun generate_returnsUrlSafeBase64WithoutPadding() {
        // Given URL-safe Base64 문자 집합
        val allowed = Regex("^[A-Za-z0-9_-]+$")

        // When nonce 를 만든다
        val nonce = generator.generate()

        // Then 패딩(=) 없는 URL-safe 문자만 들어 있다
        assertTrue(allowed.matches(nonce), "URL-safe Base64 가 아니다: $nonce")
    }

    @Test
    fun generate_returns32ByteEntropy() {
        // Given 32바이트를 패딩 없이 Base64 로 인코딩하면 43자다
        // When nonce 를 만든다
        val nonce = generator.generate()

        // Then 길이가 43이다
        assertEquals(43, nonce.length)
    }

    @Test
    fun generate_calledRepeatedly_producesDistinctValues() {
        // Given 반복 호출
        // When 100번 만든다
        val nonces = List(100) { generator.generate() }

        // Then 전부 다르다(재생 공격 방어의 전제)
        assertEquals(100, nonces.toSet().size)
    }
}
