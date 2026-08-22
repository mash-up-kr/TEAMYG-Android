package com.teamyg.parfait.domain.repository.parfaitimage

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId

interface ParfaitImageRepository {
    /**
     * 배치된 토핑을 지운다. 되돌릴 수 없다.
     *
     * 멱등이 아니라 같은 배치를 두 번 지우면 실패한다.
     */
    suspend fun deleteTopping(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit>
}
