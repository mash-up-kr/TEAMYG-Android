package com.teamyg.parfait.data.source.topping.remote

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitTextId
import com.teamyg.parfait.domain.model.topping.PlacedTextToppingVO
import com.teamyg.parfait.domain.model.topping.TextToppingStyle
import com.teamyg.parfait.domain.model.topping.TextToppingUpdate
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.UpdatedTextToppingVO

interface ParfaitTextRemoteDataSource {
    /**
     * 텍스트를 파르페 위에 배치한다.
     *
     * 이미지 배치와 달리 멱등이 아니다 — 부를 때마다 새 행이 생기고 그룹 구성원에게 푸시가 나간다.
     * 응답을 잃었다고 다시 부르면 같은 텍스트가 두 번 놓인다(`docs/api/parfait-text.md`).
     */
    suspend fun placeText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        text: String,
        transform: ToppingTransform,
        style: TextToppingStyle,
    ): Result<PlacedTextToppingVO>

    /**
     * 배치된 텍스트의 위치·크기·각도·글자 모양을 부분 수정한다. 내용은 바꿀 수 없다.
     *
     * 그룹 미참여도 본인 배치가 아닐 때와 같은 코드(PARFAIT_TEXT_NOT_OWNED, 403)가 온다.
     */
    suspend fun updateText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        update: TextToppingUpdate,
    ): Result<UpdatedTextToppingVO>

    /** 배치된 텍스트를 지운다. 멱등이 아니라 같은 배치를 두 번 지우면 404 다. */
    suspend fun deleteText(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitTextId: ParfaitTextId,
    ): Result<Unit>
}
