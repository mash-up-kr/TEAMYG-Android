package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO
import com.teamyg.parfait.domain.repository.topping.ToppingRepository
import javax.inject.Inject

class UpdateToppingUseCase @Inject constructor(
    private val toppingRepository: ToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        positionX: Double? = null,
        positionY: Double? = null,
        positionZ: Int? = null,
        scale: Double? = null,
        rotation: Double? = null,
    ): Result<UpdatedToppingVO> = toppingRepository.update(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = parfaitImageId,
        positionX = positionX,
        positionY = positionY,
        positionZ = positionZ,
        scale = scale,
        rotation = rotation,
    )
}
