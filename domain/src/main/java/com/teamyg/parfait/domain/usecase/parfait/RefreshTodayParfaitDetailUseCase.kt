package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.ParfaitId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/** 부작용 없는 상세 조회로 오늘 캔버스 캐시를 갱신한다. 쓰기 직후에 부른다 */
class RefreshTodayParfaitDetailUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    suspend operator fun invoke(
        groupId: GroupId,
        parfaitId: ParfaitId,
    ): Result<Unit> = parfaitRepository.refreshTodayCanvasDetail(groupId = groupId, parfaitId = parfaitId)
}
