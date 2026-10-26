package com.teamyg.parfait.feature.groups.canvas.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * C-106 텍스트 입력
 *
 * @param isEditingExisting 쌓인 텍스트를 다시 편집한다. 편집 대상은 NavKey 가 아니라 impl 의 세션 홀더가 나른다.
 */
@Serializable
data class NavKeyCanvasTextInput(
    val isEditingExisting: Boolean = false,
) : NavKey
