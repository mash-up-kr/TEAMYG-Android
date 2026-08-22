package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class DeleteToppingUseCase @Inject constructor(
    private val toppingRepository: ToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = toppingRepository.delete(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = parfaitImageId,
    )
}
