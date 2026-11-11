package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param welcomeGroupName 그룹 생성·참여 직후 진입이면 채워진다. 환영 배너를 1회 띄우는 데만 쓰고,
 *  그 외의 평범한 진입(그룹 목록에서 탭)에서는 `null` 이다
 * @param welcomeInviteCode [welcomeGroupName] 이 있을 때만 의미가 있다. 값이 있으면 "그룹을 만들었어요"
 *  배너를, 없으면 "그룹에 참여했어요" 배너를 보여준다
 * @param welcomeMemberLimit 생성 직후 진입에서만 채운다. 정원이 1명이면 초대할 사람이 없어 생성 배너에서
 *  초대코드와 공유 버튼을 뺀다
 */
@Serializable
data class NavKeyCanvasMain(
    val groupId: Long,
    val welcomeGroupName: String? = null,
    val welcomeInviteCode: String? = null,
    val welcomeMemberLimit: Int? = null,
) : NavKey
