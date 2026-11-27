package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 혼자 공간 토핑 배치. 배치할 토핑은 그룹 배치처럼 초안이 나른다 */
@Serializable
data object NavKeySoloToppingPlace : NavKey
