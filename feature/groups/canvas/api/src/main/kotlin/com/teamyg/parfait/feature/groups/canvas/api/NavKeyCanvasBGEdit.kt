package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 배경·토핑 편집 화면.
 *
 * [parfaitId] 를 받는 이유: 편집 대상은 언제나 **오늘의 캔버스**인데, 그 id 는 오늘 조회를
 * 이미 마친 캔버스 메인만 알고 있다. 편집 화면이 스스로 오늘 조회를 부르면 캔버스가 없는
 * 날에는 서버가 캔버스를 새로 만들어 버린다.
 *
 * @param initialToppingId 특정 토핑을 탭해 들어온 경우 그 토핑의 id
 */
@Serializable
data class NavKeyCanvasBGEdit(
    val groupId: Long,
    val parfaitId: Long,
    val initialToppingId: Long? = null,
) : NavKey
