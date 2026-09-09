package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/**
 * 토핑 만들기 흐름을 연다. 이전 초안은 통째로 덮인다
 * (`adr/0026-topping-draft-datastore-ssot.md`).
 */
class StartToppingDraftUseCase @Inject constructor(
    private val toppingDraftRepository: ToppingDraftRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        nextPositionZ: Int,
    ) = toppingDraftRepository.start(
        groupId = groupId,
        parfaitId = parfaitId,
        nextPositionZ = nextPositionZ,
    )
}
