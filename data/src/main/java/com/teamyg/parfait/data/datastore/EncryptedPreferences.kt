package com.teamyg.parfait.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.data.security.CryptoManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 암호화해 저장하고 복호화해 읽는 [DataStore] 프록시.
 *
 * 못 읽는 저장분에서 무엇을 지울지는 호출부가 `onDecodeFailure` 로 정한다. 토큰은 access 가
 * 깨지면 같은 키로 암호화된 refresh 도 버려야 하지만, 계정 정보는 자기 키 하나만 지운다.
 */
@Singleton
class EncryptedPreferences
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager,
) {
    /**
     * 복호화 전에 암호문으로 dedupe 한다. 공유 `DataStore` 라 무관한 키 변경에도 재방출하는데,
     * 복호화 뒤에 거르면 매번 Keystore 를 두드린 다음이다.
     */
    fun <T> observe(
        key: Preferences.Key<String>,
        onDecodeFailure: suspend () -> Unit = { remove(key) },
        decode: (String) -> T,
    ): Flow<T?> = dataStore.data
        .map { preferences -> preferences[key] }
        .distinctUntilChanged()
        .map { stored -> decodeOrDiscard(stored, onDecodeFailure, decode) }

    /**
     * 저장소 읽기 자체의 실패(디스크 IO 등)는 `null` 로 돌리되 폐기하지 않는다 — 일시적
     * 실패로 세션까지 잃게 된다. 폐기는 값을 읽고도 해석하지 못했을 때만이다([decodeOrDiscard]).
     */
    suspend fun <T> read(
        key: Preferences.Key<String>,
        onDecodeFailure: suspend () -> Unit = { remove(key) },
        decode: (String) -> T,
    ): T? = decodeOrDiscard(
        stored = runSuspendCatching { dataStore.data.first()[key] }.getOrNull(),
        onDecodeFailure = onDecodeFailure,
        decode = decode,
    )

    /** 한 `edit` 블록에서 써서 반쪽만 저장된 상태가 보이지 않는다. */
    suspend fun write(values: Map<Preferences.Key<String>, String>) {
        dataStore.edit { preferences ->
            values.forEach { (key, value) -> preferences[key] = cryptoManager.encrypt(value) }
        }
    }

    suspend fun write(
        key: Preferences.Key<String>,
        value: String,
    ) = write(mapOf(Pair(key, value)))

    suspend fun remove(vararg keys: Preferences.Key<String>) {
        dataStore.edit { preferences -> keys.forEach(preferences::remove) }
    }

    /**
     * 복호화·역직렬화에 실패하면(키 회전·백업 복원 등) [onDecodeFailure] 로 저장분을 버린다 —
     * 두면 매 읽기마다 같은 실패를 반복한다.
     */
    private suspend fun <T> decodeOrDiscard(
        stored: String?,
        onDecodeFailure: suspend () -> Unit,
        decode: (String) -> T,
    ): T? {
        if (stored == null) return null

        return runSuspendCatching { decode(cryptoManager.decrypt(stored)) }
            .getOrElse {
                runSuspendCatching { onDecodeFailure() }
                null
            }
    }
}
