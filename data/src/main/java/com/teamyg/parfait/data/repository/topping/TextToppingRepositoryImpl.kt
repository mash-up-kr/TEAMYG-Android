package com.teamyg.parfait.data.repository.topping

import com.teamyg.parfait.data.model.mapper.exception.mapErrorToAppError
import com.teamyg.parfait.data.source.topping.remote.ParfaitTextRemoteDataSource
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO
import com.teamyg.parfait.domain.repository.topping.TextToppingRepository
import javax.inject.Inject

class TextToppingRepositoryImpl @Inject constructor(
    private val parfaitTextRemoteDataSource: ParfaitTextRemoteDataSource,
) : TextToppingRepository {
    override suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<PlacedTextToppingVO> = parfaitTextRemoteDataSource
        .placeText(
            groupId = groupId,
            parfaitId = parfaitId,
            text = text,
            transform = transform,
            style = style,
        ).mapErrorToAppError()

    override suspend fun update(
        groupId: GroupId,
        parfaitId: ParfaitId,
        update: TextToppingUpdate,
    ): Result<UpdatedTextToppingVO> = parfaitTextRemoteDataSource
        .updateText(groupId = groupId, parfaitId = parfaitId, update = update)
        .mapErrorToAppError()

    override suspend fun delete(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitTextId: ParfaitTextId,
    ): Result<Unit> = parfaitTextRemoteDataSource
        .deleteText(groupId = groupId, parfaitId = parfaitId, parfaitTextId = parfaitTextId)
        .mapErrorToAppError()
}
