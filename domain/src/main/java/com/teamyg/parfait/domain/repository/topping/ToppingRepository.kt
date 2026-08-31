package com.teamyg.parfait.domain.repository.topping

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
import com.teamyg.parfait.domain.model.topping.ToppingTransformUpdate
import com.teamyg.parfait.domain.model.topping.UpdatedToppingBorderVO
import com.teamyg.parfait.domain.model.topping.UpdatedToppingVO

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

    /**
     * 배치된 토핑 여럿의 위치·크기·각도를 한 요청으로 부분 수정한다. 넘기지 않은 축은 서버가 유지한다.
     *
     * 부분 성공이 없다 — 하나라도 걸리면 전부 롤백되고 실패한 항목이 무엇인지는 알 수 없다.
     * [updates] 가 비면 요청 자체를 만들지 않는다.
     */
    suspend fun updateAll(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>>

    /**
     * 배치된 토핑의 테두리를 통째로 바꾼다. [updateAll]과 달리 부분 병합이 아니다.
     */
    suspend fun updateBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO>
}
