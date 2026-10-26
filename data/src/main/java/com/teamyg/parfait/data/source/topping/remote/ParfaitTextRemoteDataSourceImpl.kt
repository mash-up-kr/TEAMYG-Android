package com.teamyg.parfait.data.source.topping.remote

import com.teamyg.parfait.data.network.ApiCaller
import com.teamyg.parfait.data.service.ParfaitTextService
import com.teamyg.parfait.data.source.topping.mapper.toPlaceTextRequest
import com.teamyg.parfait.data.source.topping.mapper.toPlacedTextToppingVO
import com.teamyg.parfait.data.source.topping.mapper.toUpdateTextRequest
import com.teamyg.parfait.data.source.topping.mapper.toUpdatedTextToppingVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO
import javax.inject.Inject

class ParfaitTextRemoteDataSourceImpl @Inject constructor(
    private val parfaitTextService: ParfaitTextService,
    private val apiCaller: ApiCaller,
) : ParfaitTextRemoteDataSource {
    override suspend fun placeText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<PlacedTextToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitTextService.postGroupsByGroupIdParfaitsByParfaitIdTexts(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                request = transform.toPlaceTextRequest(text = text, style = style),
            )
        },
        transform = { it.toPlacedTextToppingVO() },
    )

    override suspend fun updateText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        update: TextToppingUpdate,
    ): Result<UpdatedTextToppingVO> = apiCaller.safeApiCall(
        block = {
            parfaitTextService.patchGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
                groupId = groupId.value,
                parfaitId = parfaitId.value,
                parfaitTextId = update.parfaitTextId.value,
                request = update.toUpdateTextRequest(),
            )
        },
        transform = { it.toUpdatedTextToppingVO() },
    )

    override suspend fun deleteText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitTextId: ParfaitTextId,
    ): Result<Unit> = apiCaller.safeApiCallWithoutData {
        parfaitTextService.deleteGroupsByGroupIdParfaitsByParfaitIdTextsByParfaitTextId(
            groupId = groupId.value,
            parfaitId = parfaitId.value,
            parfaitTextId = parfaitTextId.value,
        )
    }
}
