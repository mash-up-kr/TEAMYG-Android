package com.teamyg.parfait.data.source.member.local

import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.coroutines.flow.Flow

interface UserConfigLocalDataSource {
    /** 저장된 사용자 설정. 아직 아무것도 저장하지 않았거나 읽지 못하면 `null` */
    val userConfig: Flow<UserConfigVO?>

    suspend fun save(config: UserConfigVO)

    suspend fun clear()
}
