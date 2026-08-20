package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingDraft
import kotlinx.coroutines.flow.Flow

interface ToppingDraftRepository {
    /** 흐름 밖이면 null 이다. 가리키던 캐시 파일이 사라진 경로는 비운 채로 흐른다 */
    val draft: Flow<ToppingDraft?>

    /**
     * 흐름을 연다. 이전 초안은 남기지 않고 통째로 덮어쓴다 — 낡은 초안이 다음 흐름에
     * 따라붙는 문제를 닫는 규칙이라 별도 만료·정리 경로를 두지 않는다
     * (`adr/0026-topping-draft-datastore-ssot.md`).
     */
    suspend fun start(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    )

    suspend fun clear()
}
