package com.teamyg.parfait.data.service

import com.teamyg.parfait.data.network.NoBodyLog
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * 발급 응답 본문에는 presigned uploadUrl 이 실려 온다 — [NoBodyLog] 가 빠지면 그 URL 이
 * 디버그 빌드 로그에 그대로 찍힌다. `@NoBodyLog` 한 줄을 지워도 다른 스위트는 못 잡는다.
 */
class ImageServiceTest {
    @Test
    fun postImages_isAnnotatedWithNoBodyLog() {
        // Given·When postImages 메서드에 붙은 애노테이션들을 본다
        val method = ImageService::class.java.methods.single { it.name == "postImages" }

        // Then @NoBodyLog 가 붙어 있다
        assertTrue(method.isAnnotationPresent(NoBodyLog::class.java))
    }
}
