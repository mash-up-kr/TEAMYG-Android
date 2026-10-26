package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import javax.inject.Inject

class DeleteTextToppingUseCase @Inject constructor(
    private val textToppingRepository: TextToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitTextId: ParfaitTextId,
    ): Result<Unit> = textToppingRepository.delete(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitTextId = parfaitTextId,
    )
}
