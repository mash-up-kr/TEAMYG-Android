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
}
