package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class UpdateToppingBorderUseCase
@Inject
constructor(
    private val toppingRepository: ToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO> = toppingRepository.updateBorder(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = parfaitImageId,
        border = border,
    )
}
