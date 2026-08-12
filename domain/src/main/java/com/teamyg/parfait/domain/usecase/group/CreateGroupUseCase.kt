package com.teamyg.parfait.domain.usecase.group

import kotlinx.coroutines.delay
import javax.inject.Inject

private const val MOCK_CREATE_DURATION = 500L

class CreateGroupUseCase
@Inject
constructor() {
    // Todo : 서버 작업이 연결되면 실제 그룹 생성 API 를 호출하도록 변경 예정, 지금은 성공만 반환합니다
    suspend operator fun invoke(
        groupName: String,
        groupNumber: Int,
    ): Result<Unit> {
        delay(MOCK_CREATE_DURATION)
        return Result.success(Unit)
    }
}
