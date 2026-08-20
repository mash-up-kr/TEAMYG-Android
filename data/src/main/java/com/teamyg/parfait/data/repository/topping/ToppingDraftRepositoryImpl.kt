package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.source.toppingdraft.local.ToppingDraftLocalDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

class ToppingDraftRepositoryImpl @Inject constructor(
    private val toppingDraftLocalDataSource: ToppingDraftLocalDataSource,
) : ToppingDraftRepository {
    // 재방출 dedupe 는 원문 단계(ToppingDraftLocalDataSourceImpl.draft)에서 이미 끝났다.
    // 파일 확인만 수집자 디스패처 밖으로 옮긴다
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

    /**
     * 초안은 영속되지만 그것이 가리키는 것은 `cacheDir` 하위 파일이다. 세그멘테이션 진입이
     * 그 디렉토리를 비우고 OS 도 저장 공간이 모자라면 회수하므로, 사라진 경로는 처음부터
     * 없었던 것처럼 읽힌다(`specs/2026-08-20-c106-topping-place-api.md` 초안 SSOT 절).
     */
    private fun ToppingDraft.withExistingFilesOnly(): ToppingDraft = copy(
        subjectImagePath = subjectImagePath?.takeIf { path -> File(path).isFile },
        cutoutImagePath = cutoutImagePath?.takeIf { path -> File(path).isFile },
    )
}
