package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.ToppingDraftEntity
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.model.topping.ToppingDraft

internal fun ToppingDraft.toEntity(): ToppingDraftEntity = ToppingDraftEntity(
    groupId = groupId.value,
    parfaitId = parfaitId.value,
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
    sourceLongSide = sourceLongSide?.px,
)

internal fun ToppingDraftEntity.toVO(): ToppingDraft = ToppingDraft(
    groupId = GroupId(groupId),
    parfaitId = ParfaitId(parfaitId),
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
    sourceLongSide = sourceLongSide?.let(::SourceLongSide),
)
