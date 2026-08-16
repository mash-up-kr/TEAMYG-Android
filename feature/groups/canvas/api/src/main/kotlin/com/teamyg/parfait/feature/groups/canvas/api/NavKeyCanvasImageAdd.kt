package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * @param groupId 캔버스는 그룹 하나에 매여 있고, 오늘 캔버스 조회·상세가 모두 이 값을 받는다
 */
@Serializable
data class NavKeyCanvasImageAdd(
    val groupId: Long,
) : NavKey
