package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.canvas.CanvasVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/**
 * 특정 캔버스를 상태·멤버·배경·배치 토핑까지 한 번에 읽는다.
 *
 * 지난 날짜를 볼 때 오늘 캔버스 조회(오늘 폴러가 소유, `adr/0029-canvas-today-ssot-polling.md`)를
 * 쓰면 안 된다 — 그쪽은 캔버스가 없으면 만들어 저장한다. 이 경로는 부작용이 없다.
 *
 * `parfaitId` 는 [GetParfaitHistoriesUseCase] 가 준 목록에서 가져온다 — 날짜로 캔버스를
 * 찾는 엔드포인트는 없다.
 */
class GetParfaitDetailUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<CanvasVO> = parfaitRepository.getCanvasDetail(groupId = groupId, parfaitId = parfaitId)
}
