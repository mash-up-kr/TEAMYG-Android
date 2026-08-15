package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

/** 참여를 확정하기 전에 초대코드가 가리키는 그룹명을 확인한다 */
class GetGroupJoinPreviewUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(inviteCode: InviteCode): Result<GroupName> =
        parfaitGroupRepository.previewJoin(inviteCode)
}
