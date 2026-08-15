package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param groupId 초대코드로 참여를 마친 그룹. 이 화면에서 입력한 닉네임을 이 그룹에 적용한다
 */
@Serializable
data class NavKeyGroupNickName(val groupId: Long) : NavKey
