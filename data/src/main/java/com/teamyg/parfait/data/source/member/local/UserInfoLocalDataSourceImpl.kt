package com.teamyg.parfait.data.source.member.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.EncryptedPreferences
import com.teamyg.parfait.data.model.local.UserInfoEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserInfoLocalDataSourceImpl
@Inject
constructor(
    private val preferences: EncryptedPreferences,
    @LocalJson private val json: Json,
) : UserInfoLocalDataSource {
    // 암호문 dedupe·복호화·손상분 폐기는 EncryptedPreferences 가 한다. 여기서 정하는 것은
    // 저장 형태(JSON) 와 실패 시 지울 범위(이 키 하나)뿐이다.
    override val myAccount: Flow<MyAccountVO?> = preferences.observe(
        key = USER_INFO_KEY,
        onDecodeFailure = { clear() },
    ) { decrypted -> json.decodeFromString<UserInfoEntity>(decrypted).toVO() }

    override suspend fun save(account: MyAccountVO) =
        preferences.write(USER_INFO_KEY, json.encodeToString(account.toEntity()))

    override suspend fun clear() = preferences.remove(USER_INFO_KEY)

    internal companion object {
        const val USER_INFO_KEY_NAME = "user_info"
        val USER_INFO_KEY = stringPreferencesKey(USER_INFO_KEY_NAME)
    }
}
