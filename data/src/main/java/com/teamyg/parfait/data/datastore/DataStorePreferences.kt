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
     * 저장분을 한 번 읽는다. 없으면 `null`.
     *
     * **저장소 읽기 자체가 실패한 경우(디스크 IO 등)는 폐기하지 않는다** — 값이 손상됐다는
     * 근거가 없고, 일시적 실패로 토큰·계정 정보를 지우면 다음 시도에 살아날 세션까지 잃는다.
     * 폐기는 값을 손에 넣고도 해석하지 못했을 때만이다([decodeOrDiscard]).
     * 취소는 `null` 로 접지 않고 그대로 재던진다.
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

    /** 여러 값을 **한 `edit` 블록**에서 쓴다 — 반쪽만 저장된 상태가 보이지 않는다. */
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

    /**
     * 복호화·역직렬화에 실패하면(키 회전·백업 복원·저장 형태 손상 등) [onDecodeFailure] 로
     * 저장분을 버리고 `null` 을 돌려 재부트스트랩을 유도한다 — 영구히 못 읽는 값을 들고
     * 있어 봐야 매 읽기마다 같은 실패를 반복할 뿐이다.
     *
     * `runCatching` 이 아니라 [runSuspendCatching] 인 이유: 블록이 suspend 라 stdlib 판으로
     * 감싸면 **취소가 `null` 로 둔갑해** 화면을 벗어난 것뿐인데 "저장분 없음"으로 보고된다.
     */
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
