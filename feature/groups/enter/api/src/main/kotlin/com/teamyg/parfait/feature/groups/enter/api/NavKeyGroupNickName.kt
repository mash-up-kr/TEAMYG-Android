package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param inviteCode 참여를 확정할 때 서버로 보낼 초대코드
 * @param groupName 확인 팝업에 띄울 그룹명
 */
@Serializable
data class NavKeyGroupNickName(
    val inviteCode: String,
    val groupName: String,
) : NavKey
