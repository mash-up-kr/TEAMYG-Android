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
 * 암호화해 저장하고 복호화해 읽는 [DataStore] 프록시. 저장 형태가 값이 아니라 **암호문**인
 * 저장소들이 공유한다(`EncryptedTokenStore` · `UserInfoLocalDataSourceImpl`).
 *
 * 세 가지를 한 자리에 모은다 — 쓰기의 [CryptoManager.encrypt], 읽기의 복호화·역직렬화,
 * 그리고 **못 읽는 저장분을 버리는 복구**다. 셋 다 저장소마다 똑같이 필요하고, 특히 마지막은
 * 빠뜨려도 평소에는 드러나지 않는다(키 회전·백업 복원처럼 드물게만 실패한다).
 *
 * 저장소가 정하는 것은 **무엇을 지울지**([onDecodeFailure])와 **어떻게 해석할지**([read] 의
 * `decode`)뿐이다. 토큰은 access 하나가 깨지면 refresh 까지 같이 버려야 하지만(같은 키로
 * 암호화돼 있어 그쪽도 못 읽는다) 계정 정보는 자기 키 하나만 지운다 — 그 차이를 프록시가
 * 임의로 정하지 않게 호출부가 넘긴다.
 */
@Singleton
class EncryptedPreferences
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager,
) {
    /**
     * 저장분을 구독한다. 값이 없으면 `null` 을 방출한다.
     *
     * **암호문에서 먼저 dedupe 한 뒤 복호화한다.** 이 `DataStore` 는 여러 저장소가 공유해서
     * `data` 가 무관한 키 변경(예: 토큰 재발급 저장)에도 재방출하는데, 복호화 뒤에 dedupe
     * 하면 이미 매번 Keystore 를 두드린 다음이라 비용을 못 던다. 값이 실제로 안 바뀌었으면
     * 구독자(예: 편집 중인 입력 필드)도 조용히 넘어간다.
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

        return runSuspendCatching { decode(cryptoManager.decrypt(stored)) }
            .getOrElse {
                runSuspendCatching { onDecodeFailure() }
                null
            }
    }
}
