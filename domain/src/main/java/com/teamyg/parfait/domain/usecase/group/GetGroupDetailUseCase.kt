package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.GroupDetailVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    /**
     * TODO(서버 응답 확장 대기): 그룹 상세에 그룹명이 없어 목록 캐시에서 이름만 집어 붙인다.
     *  서버가 상세에 groupName 을 실어 주면 이 [combine] 을 걷어낸다.
     *
     * 이름을 못 찾아도 상세를 접지 않는다 — 이름 한 줄 때문에 멤버·초대코드까지 못 보여 주는
     * 것보다, 이름을 비우고 나머지를 띄우는 편이 낫다.
     */
    operator fun invoke(groupId: GroupId): Flow<GroupDetailVO?> = combine(
        parfaitGroupRepository.groupDetail(groupId),
        parfaitGroupRepository.myGroups,
    ) { detail, groups ->
        detail?.let {
            GroupDetailVO(
                groupId = it.groupId,
                groupName = groups
                    ?.firstOrNull { group -> group.groupId == groupId }
                    ?.groupName
                    ?: GroupName(""),
                myNickname = it.groupNickname,
                inviteCode = it.inviteCode,
                members = it.members,
            )
        }
    }
}
