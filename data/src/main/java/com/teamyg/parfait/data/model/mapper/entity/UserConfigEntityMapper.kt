package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.UserConfigEntity
import com.teamyg.parfait.domain.model.member.TutorialKind
import com.teamyg.parfait.domain.model.member.UserConfigVO

internal fun UserConfigVO.toEntity(): UserConfigEntity = UserConfigEntity(
    seenTutorials = seenTutorials.mapTo(mutableSetOf(), TutorialKind::name),
)

internal fun UserConfigEntity.toVO(): UserConfigVO = UserConfigVO(
    seenTutorials = seenTutorials.mapNotNullTo(mutableSetOf()) { name ->
        TutorialKind.entries.firstOrNull { it.name == name }
    },
)
