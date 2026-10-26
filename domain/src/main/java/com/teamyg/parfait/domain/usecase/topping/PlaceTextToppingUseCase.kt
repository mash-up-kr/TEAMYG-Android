package com.teamyg.parfait.domain.usecase.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import javax.inject.Inject

class PlaceTextToppingUseCase @Inject constructor(
    private val textToppingRepository: TextToppingRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<PlacedTextToppingVO> = textToppingRepository.place(
        groupId = groupId,
        parfaitId = parfaitId,
        text = text,
        transform = transform,
        style = style,
    )
}
