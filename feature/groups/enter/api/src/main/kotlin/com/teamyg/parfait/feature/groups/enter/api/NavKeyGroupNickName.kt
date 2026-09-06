package com.teamyg.parfait.feature.groups.enter.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param inviteCode 참여를 확정할 때 서버로 보낼 초대코드
 * @param groupName 확인 팝업에 띄울 그룹명
 * @param nickName 그룹 닉네임이 아니라 초기값으로 채워 둘 **앱** 닉네임. 구하지 못했으면 빈 문자열이다
 */
@Serializable
data class NavKeyGroupNickName(
    val inviteCode: String,
    val groupName: String,
    val nickName: String,
) : NavKey
