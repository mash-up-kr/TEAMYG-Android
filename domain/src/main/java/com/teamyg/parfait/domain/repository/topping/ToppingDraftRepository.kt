package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow

interface ToppingDraftRepository {
    /** 가리키던 캐시 파일이 사라진 경로는 비운 채로 흐른다 */
    val draft: Flow<ToppingDraft?>

    /**
     * 흐름을 연다. 이전 초안은 남기지 않고 통째로 덮어쓴다 — 낡은 초안이 따라붙는 문제를 이
     * 규칙 하나로 닫는다(`adr/0026-topping-draft-datastore-ssot.md`).
     */
    suspend fun start(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    )

    suspend fun clear()

    /**
     * 흐름이 만들어 낸 알맹이·테두리를 초안에 적는다. 캔버스 식별값은 진입 때 못 박은 것을 그대로 둔다.
     *
     * 테두리는 넘어온 값으로 매번 덮어쓴다 — 알맹이가 바뀌면 그 전 테두리는 설 자리가 없다.
     *
     * @return 흐름이 열려 있지 않으면 `false`. 없는 초안을 지어내지 않는다
     */
    suspend fun record(
        subjectImagePath: String,
        cutoutImagePath: String?,
        borderColorArgb: Int?,
        borderWidthDp: Float?,
    ): Boolean
}
