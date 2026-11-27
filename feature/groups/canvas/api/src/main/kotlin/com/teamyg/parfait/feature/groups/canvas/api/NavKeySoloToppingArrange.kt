package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 혼자 공간에 이미 놓인 토핑의 배치·테두리를 고치는 화면.
 *
 * @param initialToppingId 캔버스에서 탭한 토핑. 이 토핑을 고른 채로 연다
 * @param initialIsText [initialToppingId] 가 텍스트 id 인지
 */
@Serializable
data class NavKeySoloToppingArrange(
    val initialToppingId: Long,
    val initialIsText: Boolean,
) : NavKey
