package com.teamyg.parfait.data.source.member.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.data.model.local.UserInfoEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.data.security.CryptoManager
import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInfoLocalDataSourceImpl
@Inject
constructor(
    private val dataStore: DataStore<Preferences>,
    @LocalJson private val json: Json,
    private val cryptoManager: CryptoManager,
) : UserInfoLocalDataSource {
    override val myAccount: Flow<MyAccountVO?> = dataStore.data.map { preferences ->
        decode(preferences[USER_INFO_KEY])
    }

    override suspend fun save(account: MyAccountVO) {
        val encoded = json.encodeToString(account.toEntity())
        dataStore.edit { preferences ->
            preferences[USER_INFO_KEY] = cryptoManager.encrypt(encoded)
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences -> preferences.remove(USER_INFO_KEY) }
    }

    /**
     * 복호화·역직렬화에 실패하면(키 회전·백업 복원·저장 형태 손상 등) 저장분을 버리고
     * `null` 을 돌려 재부트스트랩을 유도한다 — [EncryptedTokenStore.read] 와 같은 패턴.
     *
     * `runCatching` 이 아니라 [runSuspendCatching] 인 이유: 블록 안에서 `clear()` 를
     * suspend 로 호출하므로, stdlib 판으로 감싸면 **취소가 `null` 로 둔갑해** 화면을
     * 벗어난 것뿐인데 "저장분 없음"으로 보고된다.
     *
     * [Flow.map] 의 transform 은 suspend 람다라 여기서 곧바로 `clear()` 를 부를 수
     * 있다 — 별도의 정리 경로를 만들 필요가 없다.
     */
    private suspend fun decode(stored: String?): MyAccountVO? {
        if (stored == null) return null

        return runSuspendCatching {
            val decrypted = cryptoManager.decrypt(stored)
            json.decodeFromString<UserInfoEntity>(decrypted).toVO()
        }.getOrElse {
            runSuspendCatching { clear() }
            null
        }
    }

    internal companion object {
        const val USER_INFO_KEY_NAME = "user_info"
        val USER_INFO_KEY = stringPreferencesKey(USER_INFO_KEY_NAME)
    }
}
