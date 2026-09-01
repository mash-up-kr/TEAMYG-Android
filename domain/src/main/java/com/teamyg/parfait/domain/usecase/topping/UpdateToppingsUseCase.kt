package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class UpdateToppingsUseCase @Inject constructor(
    private val toppingRepository: ToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>> = toppingRepository.updateAll(
        groupId = groupId,
        parfaitId = parfaitId,
        updates = updates,
    )
}
