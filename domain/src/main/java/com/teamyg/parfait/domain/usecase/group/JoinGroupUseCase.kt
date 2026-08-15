package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.JoinedGroupVO
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import javax.inject.Inject

class JoinGroupUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    suspend operator fun invoke(inviteCode: InviteCode): Result<JoinedGroupVO> =
        parfaitGroupRepository.joinGroup(inviteCode)
}
