package com.teamyg.parfait.data.source.parfaitimage.remote

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ImageId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.model.topping.PlacedToppingVO
import com.teamyg.parfait.domain.model.topping.ToppingBorder
import com.teamyg.parfait.domain.model.topping.ToppingTransform
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
     * 배치된 토핑의 위치·크기·각도를 부분 수정한다. 넘기지 않은 값은 서버가 유지한다.
     *
     * 테두리는 이 API 로 바꿀 수 없다 — 서버 요청에 필드가 없다. 바꾸려면 같은 imageId 로
     * placeTopping 을 다시 부르는 수밖에 없고, 그 경로는 소유자를 덮어쓴다.
     *
     * 그룹에 참여하지 않았을 때도 본인 배치가 아닐 때와 같은 코드(PARFAIT_IMAGE_NOT_OWNED,
     * 403)가 온다 — placeTopping 이 미참여를 GROUP_NOT_JOINED 로 구분하는 것과 다르다.
     */
    suspend fun updateTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
        positionX: Double? = null,
        positionY: Double? = null,
        positionZ: Int? = null,
        scale: Double? = null,
        rotation: Double? = null,
    ): Result<UpdatedToppingVO>
}
