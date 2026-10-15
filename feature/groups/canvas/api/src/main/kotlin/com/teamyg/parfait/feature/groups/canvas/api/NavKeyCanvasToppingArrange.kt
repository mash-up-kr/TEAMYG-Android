package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 이미 캔버스에 놓인 본인 토핑의 배치·테두리를 고치는 화면.
 *
 * [parfaitId] 를 받는 이유는 [NavKeyCanvasBGEdit] 와 같다.
 *
 * @param initialToppingId 캔버스에서 탭한 토핑. 이 토핑을 고른 채로 연다
 */
@Serializable
data class NavKeyCanvasToppingArrange(
    val groupId: Long,
    val parfaitId: Long,
    val initialToppingId: Long,
) : NavKey
