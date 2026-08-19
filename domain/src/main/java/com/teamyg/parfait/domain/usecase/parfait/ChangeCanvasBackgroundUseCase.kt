package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasBackground
import com.teamyg.parfait.domain.model.canvas.CanvasBackgroundEdit
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/**
 * 캔버스 배경을 바꾸고 저장된 배경을 돌려받는다.
 *
 * 반환값을 버리지 않는 이유: 이미지 배경은 앱이 imageId 만 알고 URL 은 모른다 — 방금 저장한
 * 배경을 그리려면 이 응답이 유일한 출처다. 서버가 앱이 모르는 type 을 돌려주면 null 이다
 * (저장은 됐지만 그릴 수 없다).
 */
class ChangeCanvasBackgroundUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
        background: CanvasBackgroundEdit,
    ): Result<CanvasBackground?> = parfaitRepository.changeCanvasBackground(
        groupId = groupId,
        parfaitId = parfaitId,
        background = background,
    )
}
