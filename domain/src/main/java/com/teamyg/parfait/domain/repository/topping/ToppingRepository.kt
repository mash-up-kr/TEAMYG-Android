package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform

interface ToppingRepository {
    /**
     * 업로드가 확정된 이미지를 파르페 위 좌표에 배치한다.
     *
     * 이미 배치된 imageId 로 다시 부를 때의 upsert 와 소유자 이전은 `api/parfait-image.md` 참고.
     *
     * @param transform 화면 좌표가 아니라 정규화된 서버 좌표다.
     */
    suspend fun place(
        groupId: GroupId,
        parfaitId: ParfaitId,
        imageId: ImageId,
        transform: ToppingTransform,
        border: ToppingBorder,
    ): Result<PlacedToppingVO>

    /**
     * 배치된 토핑을 지운다. 되돌릴 수 없다.
     *
     * 멱등이 아니라 같은 배치를 두 번 지우면 실패한다.
     */
    suspend fun delete(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit>
}
