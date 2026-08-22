package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class ToppingDraftRepositoryImpl @Inject constructor(
    private val toppingDraftLocalDataSource: ToppingDraftLocalDataSource,
) : ToppingDraftRepository {
    override val draft: Flow<ToppingDraft?> = toppingDraftLocalDataSource.draft
        .map { draft -> draft?.withExistingFilesOnly() }
        .flowOn(Dispatchers.IO)

    override suspend fun start(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    ) = toppingDraftLocalDataSource.save(
        ToppingDraft(
            groupId = groupId,
            parfaitId = parfaitId,
            nextPositionZ = nextPositionZ,
        ),
    )

    override suspend fun clear() = toppingDraftLocalDataSource.clear()

    // 정규화된 [draft] 가 아니라 원문을 읽는다 — 여기서 필요한 것은 캔버스 식별값뿐이고,
    // 정규화는 파일 존재 확인 IO 를 태운다
    override suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean {
        val current = toppingDraftLocalDataSource.draft.first() ?: return false

        toppingDraftLocalDataSource.save(
            current.copy(
                subjectImagePath = subjectImagePath,
                cutoutImagePath = cutoutImagePath,
                borderColorArgb = borderColorArgb,
                borderWidthDp = borderWidthDp,
            ),
        )
        return true
    }

    /**
     * 초안은 영속되지만 그것이 가리키는 것은 `cacheDir` 하위 파일이라 먼저 사라질 수 있다
     * (`specs/2026-08-20-c106-topping-place-api.md` 초안 SSOT 절).
     */
    private fun ToppingDraft.withExistingFilesOnly(): ToppingDraft = copy(
        subjectImagePath = subjectImagePath?.takeIf { path -> File(path).isFile },
        cutoutImagePath = cutoutImagePath?.takeIf { path -> File(path).isFile },
    )
}
