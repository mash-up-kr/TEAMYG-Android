package com.teamyg.parfait.data.source.member.local

import com.teamyg.parfait.domain.model.member.MyAccountVO
import kotlinx.coroutines.flow.Flow

interface UserInfoLocalDataSource {
    /** 저장된 계정 정보. 없거나 복호화에 실패하면 `null` */
    val myAccount: Flow<MyAccountVO?>

    suspend fun save(account: MyAccountVO)

    suspend fun clear()
}
