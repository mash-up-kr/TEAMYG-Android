package com.teamyg.parfait.domain.model.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId

/** 토핑 초안이 놓일 공간. 공유 카메라·갤러리 흐름이 끝나고 어느 배치 화면으로 갈지를 가른다 */
sealed interface ToppingDraftTarget {
    data class Group(val groupId: GroupId, val parfaitId: ParfaitId) : ToppingDraftTarget

    data object Solo : ToppingDraftTarget
}
