package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param inviteCode 참여를 확정할 때 서버로 보낼 초대코드. 참여 요청은 이 화면의 확인 팝업에서 나간다
 * @param groupName 확인 팝업에 띄울 그룹명. 앞 화면이 join-preview 로 확인해 둔 값이다
 */
@Serializable
data class NavKeyGroupNickName(
    val inviteCode: String,
    val groupName: String,
) : NavKey
