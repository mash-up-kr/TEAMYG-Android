package com.teamyg.parfait.core.util.jvm.extension

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UriExtensionTest {
    @Test
    fun toJavaUriOrNull_validUri_parsesSchemeAndHost() {
        val uri = "https://play.google.com/store/apps/details?id=x".toJavaUriOrNull()

        assertEquals("https", uri?.scheme)
        assertEquals("play.google.com", uri?.host)
    }

    @Test
    fun toJavaUriOrNull_malformed_isNull() {
        // Given 공백이 섞여 URI 문법에 맞지 않는 문자열
        assertNull("not a url".toJavaUriOrNull())
    }

    @Test
    fun toJavaUriOrNull_empty_hasNoScheme() {
        // Then 빈 문자열은 예외 없이 스킴 없는 URI 다
        assertNull("".toJavaUriOrNull()?.scheme)
    }
}
