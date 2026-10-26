package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import javax.inject.Inject

class UpdateTextToppingUseCase @Inject constructor(
    private val textToppingRepository: TextToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        update: TextToppingUpdate,
    ): Result<UpdatedTextToppingVO> = textToppingRepository.update(
        groupId = groupId,
        parfaitId = parfaitId,
        update = update,
    )
}
