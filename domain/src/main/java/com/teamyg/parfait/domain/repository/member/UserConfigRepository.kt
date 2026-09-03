package com.teamyg.parfait.domain.repository.member

import com.teamyg.parfait.domain.model.member.UserConfigVO
import kotlinx.coroutines.flow.Flow

interface UserConfigRepository {
    /** 기기에 저장된 사용자 설정. 아직 아무것도 저장하지 않았으면 `null` */
    val userConfig: Flow<UserConfigVO?>

    suspend fun updateIsShowCanvasTutorial(isShowCanvasTutorial: Boolean)

    suspend fun clearConfig()
}
