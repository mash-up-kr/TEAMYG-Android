package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.group.ParfaitGroupRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 캐시된 그룹 상세를 구독한다. 값을 새로 받는 것은 [RefreshGroupDetailUseCase] 의 일이다.
 *
 * 서버가 상세 응답에 그룹명·정원을 실어 주기 전에는 목록 캐시에서 이름만 집어 붙였는데
 * (서버 `08df1bf`), 지금은 상세 하나로 화면이 채워진다.
 */
class GetGroupDetailUseCase @Inject constructor(
    private val parfaitGroupRepository: ParfaitGroupRepository,
) {
    operator fun invoke(groupId: GroupId): Flow<ParfaitGroupDetailVO?> =
        parfaitGroupRepository.groupDetail(groupId)
}
