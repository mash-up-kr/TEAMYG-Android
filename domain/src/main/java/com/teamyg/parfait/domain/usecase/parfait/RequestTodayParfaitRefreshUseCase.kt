package com.teamyg.parfait.domain.usecase.parfait

import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.repository.parfait.ParfaitRepository
import javax.inject.Inject

/** 화면이 곧 사라지는 자리에서 부른다 — 결과를 기다리지 않고 저장소 층이 마저 끝낸다 */
class RequestTodayParfaitRefreshUseCase
@Inject
constructor(
    private val parfaitRepository: ParfaitRepository,
) {
    operator fun invoke(groupId: GroupId) = parfaitRepository.requestTodayCanvasRefresh(groupId)
}
