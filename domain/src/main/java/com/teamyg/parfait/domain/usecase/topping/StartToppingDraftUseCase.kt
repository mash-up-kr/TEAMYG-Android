package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.topping.ToppingDraftRepository
import javax.inject.Inject

/** 이전 초안을 남기지 않고 통째로 덮는다(`adr/0026-topping-draft-datastore-ssot.md`) */
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
