package com.teamyg.parfait.data.source.token.local

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.EncryptedPreferences
import javax.inject.Inject

class EncryptedTokenStore
@Inject
constructor(
    private val preferences: EncryptedPreferences,
) : TokenStore {
    override suspend fun getAccessToken(): String? = read(ACCESS_TOKEN_KEY)

    override suspend fun getRefreshToken(): String? = read(REFRESH_TOKEN_KEY)

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
    ) = preferences.write(
        mapOf(
            Pair(ACCESS_TOKEN_KEY, accessToken),
            Pair(REFRESH_TOKEN_KEY, refreshToken),
        ),
    )

    override suspend fun clear() = preferences.remove(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY)

    /**
     * 복호화에 실패하면(키 회전·백업 복원 등) 저장분을 버리고 `null` 을 돌려 재로그인을
     * 유도한다 — [com.teamyg.parfait.data.security.CryptoManager] 참고.
     *
     * 한 짝 전체([clear])를 버린다. 두 토큰이 같은 키로 암호화돼 있어 하나를 못 읽으면 다른 하나도 못 읽는다.
     */
    private suspend fun read(key: Preferences.Key<String>): String? =
        preferences.read(key, onDecodeFailure = { clear() }) { it }

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
