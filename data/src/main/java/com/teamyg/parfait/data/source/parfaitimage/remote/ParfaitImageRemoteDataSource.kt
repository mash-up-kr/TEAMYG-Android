package com.teamyg.parfait.data.source.parfaitimage.remote

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

    /**
     * 배치된 토핑 여럿의 위치·크기·각도를 한 요청으로 부분 수정한다. 넘기지 않은 축은 서버가 유지한다.
     *
     * 부분 성공이 없다 — 항목 하나가 걸리면 전부 롤백되고 어느 항목이었는지는 응답에 없다.
     * 테두리는 이 API 로 바꿀 수 없다(요청에 필드가 없다) — [updateToppingBorder] 가 맡는다.
     *
     * 그룹에 참여하지 않았을 때도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED,
     * 403)가 온다. 그룹 멤버라면 마감된 캔버스가 항목별 소유권보다 먼저 걸려 409
     * PARFAIT_ALREADY_CLOSED 다 — 그 둘의 순서가 단건 수정과 반대다(`api/parfait-image.md`).
     */
    suspend fun updateToppings(
        groupId: GroupId,
        parfaitId: ParfaitId,
        updates: List<ToppingTransformUpdate>,
    ): Result<List<UpdatedToppingVO>>

    /**
     * 배치된 토핑의 테두리를 바꾼다.
     *
     * 위치 수정과 달리 부분 병합이 아니라 통째 덮기다 — 그래서 nullable 파라미터가 아니라
     * ToppingBorder 하나를 받는다. sealed 라 SOLID 인데 색·두께가 빠지는 조합을 만들 수 없고,
     * 그래서 400 INVALID_BORDER 는 앱에서 도달 불가다.
     *
     * 그룹 미참여도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED, 403)가 온다.
     */
    suspend fun updateToppingBorder(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        border: ToppingBorder,
    ): Result<UpdatedToppingBorderVO>

    /**
     * 배치된 토핑을 지운다. 되돌릴 수 없다.
     *
     * 서버가 배치 행을 지우면서 이미지 참조 수를 줄이고, 그것이 0이 되면 S3 객체까지 지운다
     * (`api/parfait-image.md`). 멱등이 아니라 같은 배치를 두 번 지우면 404 다.
     */
    suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit>
}
