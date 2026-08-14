package com.teamyg.parfait.data.source.token.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.data.security.CryptoManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class EncryptedTokenStore
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    private val cryptoManager: CryptoManager,
) : TokenStore {
    override suspend fun getAccessToken(): String? = read(ACCESS_TOKEN_KEY)

    override suspend fun getRefreshToken(): String? = read(REFRESH_TOKEN_KEY)

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN_KEY] = cryptoManager.encrypt(accessToken)
            preferences[REFRESH_TOKEN_KEY] = cryptoManager.encrypt(refreshToken)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }

    /**
     * 복호화에 실패하면(키 회전·백업 복원 등) 저장분을 버리고 `null` 을 돌려 재로그인을
     * 유도한다 — [com.teamyg.parfait.data.security.CryptoManager] 참고.
     *
     * `runCatching` 이 아니라 [runSuspendCatching] 인 이유: `dataStore.data.first()` 가
     * suspend 라, stdlib 판으로 감싸면 **취소가 `null` 로 둔갑해 호출부에 "토큰 없음"으로
     * 보고된다.** 화면을 벗어난 것뿐인데 로그아웃 상태로 읽히는 경로였다.
     */
    private suspend fun read(key: Preferences.Key<String>): String? = runSuspendCatching {
        val encrypted = dataStore.data.first()[key] ?: return@runSuspendCatching null
        cryptoManager.decrypt(encrypted)
    }.getOrElse {
        runSuspendCatching { clear() }
        null
    }

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
