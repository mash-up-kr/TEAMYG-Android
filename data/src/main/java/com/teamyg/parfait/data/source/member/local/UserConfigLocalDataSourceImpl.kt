package com.teamyg.parfait.data.source.member.local

import androidx.datastore.preferences.core.stringPreferencesKey
import com.teamyg.parfait.data.datastore.DataStorePreferences
import com.teamyg.parfait.data.model.local.UserConfigEntity
import com.teamyg.parfait.data.model.local.toEntity
import com.teamyg.parfait.data.model.local.toVO
import com.teamyg.parfait.data.model.qualifier.LocalJson
import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserConfigLocalDataSourceImpl
@Inject
constructor(
    private val preferences: DataStorePreferences,
    @LocalJson private val json: Json,
) : UserConfigLocalDataSource {
    // 계정 정보([UserInfoLocalDataSourceImpl])와 달리 암호화하지 않는다 — 담기는 것이 "튜토리얼을
    // 봤는가" 뿐이라 지킬 것이 없고, 키 회전 한 번에 설정이 통째로 폐기될 위험만 남는다
    override val userConfig: Flow<UserConfigVO?> = preferences.observe(
        key = USER_CONFIG_KEY,
        onDecodeFailure = { clear() },
    ) { stored -> json.decodeFromString<UserConfigEntity>(stored).toVO() }

    override suspend fun save(config: UserConfigVO) =
        preferences.write(USER_CONFIG_KEY, json.encodeToString(config.toEntity()))

    override suspend fun clear() = preferences.remove(USER_CONFIG_KEY)

    internal companion object {
        const val USER_CONFIG_KEY_NAME = "user_config"
        val USER_CONFIG_KEY = stringPreferencesKey(USER_CONFIG_KEY_NAME)
    }
}
