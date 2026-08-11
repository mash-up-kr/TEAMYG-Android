package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform

interface ParfaitImageRemoteDataSource {
    /**
     * 업로드가 확인된(COMPLETED) 이미지를 파르페 위 좌표에 배치한다.
     *
     * 같은 (parfaitId, imageId) 로 다시 부르면 새 배치가 생기지 않고 기존 배치가
     * 이동하며 소유자가 호출자로 바뀐다 — 서버가 upsert 로 구현돼 있고 배치자를
     * 대조하지 않는다(`api/parfait-image.md`). 같은 이미지를 두 번 배치할 수 없다.
     */
    suspend fun placeTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO>
}
