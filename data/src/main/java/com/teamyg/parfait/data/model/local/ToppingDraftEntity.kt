package com.teamyg.parfait.data.model.local

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.serialization.Serializable

/** 초안의 저장 형태. 값 클래스를 품고 있어 domain 이 직렬화를 알게 하지 않는다(`adr/0001-layered-multi-module.md`) */
@Serializable
internal data class ToppingDraftEntity(
    val groupId: Long,
    val parfaitId: Long,
    val nextPositionZ: Int,
    val subjectImagePath: String? = null,
    val cutoutImagePath: String? = null,
    val borderColorArgb: Int? = null,
    val borderWidthDp: Float? = null,
)

internal fun ToppingDraft.toEntity(): ToppingDraftEntity = ToppingDraftEntity(
    groupId = groupId.value,
    parfaitId = parfaitId.value,
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
)

internal fun ToppingDraftEntity.toVO(): ToppingDraft = ToppingDraft(
    groupId = GroupId(groupId),
    parfaitId = ParfaitId(parfaitId),
    nextPositionZ = nextPositionZ,
    subjectImagePath = subjectImagePath,
    cutoutImagePath = cutoutImagePath,
    borderColorArgb = borderColorArgb,
    borderWidthDp = borderWidthDp,
)
