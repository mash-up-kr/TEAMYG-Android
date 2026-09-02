package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param welcomeGroupName 그룹 생성·참여 직후 진입이면 채워진다. 환영 배너를 1회 띄우는 데만 쓰고,
 *  그 외의 평범한 진입(그룹 목록에서 탭)에서는 `null` 이다
 * @param welcomeInviteCode [welcomeGroupName] 이 있을 때만 의미가 있다. 값이 있으면 "그룹을 만들었어요"
 *  배너(초대코드 복사 버튼 포함)를, 없으면 "그룹에 참여했어요" 배너를 보여준다
 */
@Serializable
data class NavKeyCanvasMain(
    val groupId: Long,
    val welcomeGroupName: String? = null,
    val welcomeInviteCode: String? = null,
) : NavKey
