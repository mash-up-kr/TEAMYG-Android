package com.teamyg.parfait.domain.usecase.parfaitimage

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.model.id.ParfaitImageId
import com.teamyg.parfait.domain.repository.parfaitimage.ParfaitImageRepository
import javax.inject.Inject

class DeleteToppingUseCase @Inject constructor(
    private val parfaitImageRepository: ParfaitImageRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        parfaitImageId: ParfaitImageId,
    ): Result<Unit> = parfaitImageRepository.deleteTopping(
        groupId = groupId,
        parfaitId = parfaitId,
        parfaitImageId = parfaitImageId,
    )
}
