package com.teamyg.parfait.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.teamyg.parfait.domain.model.id.MemberId
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 계정 id 를 저장 키 이름에 그대로 쓰지 않으려고 만드는 별칭.
 *
 * `memberId` 는 작은 정수라 단순 해시는 1부터 대입해 역산된다. 그래서 기기 밖으로 나가지
 * 않는 Keystore HMAC 키로 서명한다 — Preferences 파일만 봐서는 별칭에서 id 를 복원할 수 없다.
 * 백업이 다른 기기로 복원되면 이 키가 없어 별칭이 달라지고 복원된 기록은 어느 계정과도
 * 맞지 않게 되는데, 의도한 동작이다.
 */
@Singleton
class AccountKeyAlias
@Inject
constructor() {
    fun alias(
        scope: String,
        memberId: MemberId,
    ): String {
        val mac = Mac.getInstance(MAC_ALGORITHM).apply { init(getOrCreateKey()) }
        val signature = mac.doFinal("$scope:${memberId.value}".toByteArray(Charsets.UTF_8))
        return signature.joinToString(separator = "") { "%02x".format(it) }.take(ALIAS_LENGTH)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "parfait_alias_key"
        const val MAC_ALGORITHM = "HmacSHA256"
        const val ALIAS_LENGTH = 16
    }
}
