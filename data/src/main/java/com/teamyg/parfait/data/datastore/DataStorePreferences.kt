package com.teamyg.parfait.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStorePreferences
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
) {
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
     * 실패일 수 있다. 폐기는 값을 읽고도 해석하지 못했을 때만이다([decodeOrDiscard]).
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
            values.forEach { (key, value) -> preferences[key] = value }
        }
    }

    suspend fun write(
        key: Preferences.Key<String>,
        value: String,
    ) = write(mapOf(Pair(key, value)))

    suspend fun remove(vararg keys: Preferences.Key<String>) {
        dataStore.edit { preferences -> keys.forEach(preferences::remove) }
    }

    /** 해석에 실패하면 [onDecodeFailure] 로 저장분을 버린다 — 두면 매 읽기마다 같은 실패를 반복한다. */
    private suspend fun <T> decodeOrDiscard(
        stored: String?,
        onDecodeFailure: suspend () -> Unit,
        decode: (String) -> T,
    ): T? {
        if (stored == null) return null

        return runSuspendCatching { decode(stored) }
            .getOrElse {
                runSuspendCatching { onDecodeFailure() }
                null
            }
    }
}
