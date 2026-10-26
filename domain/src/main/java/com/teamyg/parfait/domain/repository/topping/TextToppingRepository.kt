package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO

interface TextToppingRepository {
    /**
     * 멱등이 아니다 — 부를 때마다 새 행이 생기고 푸시가 나간다. 응답을 잃었다고 다시 부르면
     * 같은 텍스트가 두 번 놓인다(`api/parfait-text.md`).
     *
     * @param transform 화면 좌표가 아니라 정규화된 서버 좌표다.
     */
    suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<PlacedTextToppingVO>

    suspend fun update(
        groupId: GroupId,
        parfaitId: ParfaitId,
        update: TextToppingUpdate,
    ): Result<UpdatedTextToppingVO>

    suspend fun delete(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitTextId: ParfaitTextId,
    ): Result<Unit>
}
