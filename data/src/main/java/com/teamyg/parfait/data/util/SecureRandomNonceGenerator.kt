package com.teamyg.parfait.data.util

import com.teamyg.parfait.domain.util.NonceGenerator
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject

private const val NONCE_BYTE_SIZE = 32

/** [SecureRandom] 32바이트를 패딩 없는 URL-safe Base64(43자)로 인코딩한다 */
class SecureRandomNonceGenerator @Inject constructor() : NonceGenerator {
    private val secureRandom = SecureRandom()

    override fun generate(): String {
        val bytes = ByteArray(NONCE_BYTE_SIZE).also(secureRandom::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
