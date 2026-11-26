package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** 혼자 공간 텍스트 입력. 저장 대상은 그룹 텍스트 입력처럼 초안이 나른다 */
@Serializable
data object NavKeySoloTextInput : NavKey
