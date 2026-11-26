package com.teamyg.parfait.data.model.mapper.entity

import com.teamyg.parfait.data.model.entity.ToppingDraftEntity
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.image.SourceLongSide
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.model.topping.ToppingDraftTarget

internal fun ToppingDraft.toEntity(): ToppingDraftEntity = when (val draftTarget = target) {
    is ToppingDraftTarget.Group -> ToppingDraftEntity(
        groupId = draftTarget.groupId.value,
        parfaitId = draftTarget.parfaitId.value,
        isSolo = false,
        nextPositionZ = nextPositionZ,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        sourceLongSide = sourceLongSide?.px,
    )

    ToppingDraftTarget.Solo -> ToppingDraftEntity(
        isSolo = true,
        nextPositionZ = nextPositionZ,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        sourceLongSide = sourceLongSide?.px,
    )
}

/** 그룹인데 id 가 비어 있으면 읽을 수 없는 초안이라 `null`(초안 없음)이다 */
internal fun ToppingDraftEntity.toVO(): ToppingDraft? {
    val target = when {
        isSolo -> ToppingDraftTarget.Solo
        groupId != null && parfaitId != null ->
            ToppingDraftTarget.Group(GroupId(groupId), ParfaitId(parfaitId))
        else -> return null
    }

    return ToppingDraft(
        target = target,
        nextPositionZ = nextPositionZ,
        subjectImagePath = subjectImagePath,
        cutoutImagePath = cutoutImagePath,
        sourceLongSide = sourceLongSide?.let(::SourceLongSide),
    )
}
