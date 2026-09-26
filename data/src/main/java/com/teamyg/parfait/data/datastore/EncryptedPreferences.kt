package com.teamyg.parfait.data.datastore

import androidx.datastore.preferences.core.Preferences
import com.teamyg.parfait.data.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [DataStorePreferences] 위에 암호화를 얹는다. 쓸 때 암호화하고, 복호화는 `decode` 안에서 해
 * 복호화 실패도 [DataStorePreferences] 의 폐기 경로를 탄다.
 *
 * 못 읽는 저장분에서 무엇을 지울지는 호출부가 `onDecodeFailure` 로 정한다. 토큰은 access 가
 * 깨지면 같은 키로 암호화된 refresh 도 버려야 하지만, 계정 정보는 자기 키 하나만 지운다.
 */
@Singleton
class EncryptedPreferences
@Inject
constructor(
    private val preferences: DataStorePreferences,
    private val cryptoManager: CryptoManager,
) {
    /**
     * dedupe 는 복호화 전 암호문에서 일어난다. 공유 `DataStore` 라 무관한 키 변경에도 재방출하는데,
     * 복호화 뒤에 거르면 매번 Keystore 를 두드린 다음이다.
     */
    fun <T> observe(
        key: Preferences.Key<String>,
        onDecodeFailure: suspend () -> Unit = { remove(key) },
        decode: (String) -> T,
    ): Flow<T?> = preferences.observe(key, onDecodeFailure) { decode(cryptoManager.decrypt(it)) }

    suspend fun <T> read(
        key: Preferences.Key<String>,
        onDecodeFailure: suspend () -> Unit = { remove(key) },
        decode: (String) -> T,
    ): T? = preferences.read(key, onDecodeFailure) { decode(cryptoManager.decrypt(it)) }

    suspend fun write(values: Map<Preferences.Key<String>, String>) =
        preferences.write(values.mapValues { (_, value) -> cryptoManager.encrypt(value) })

    suspend fun write(
        key: Preferences.Key<String>,
        value: String,
    ) = write(mapOf(Pair(key, value)))

    suspend fun remove(vararg keys: Preferences.Key<String>) = preferences.remove(*keys)
}
