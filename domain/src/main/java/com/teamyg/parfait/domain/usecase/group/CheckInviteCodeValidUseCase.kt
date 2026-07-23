package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.InviteCodeResult
import kotlinx.coroutines.delay
import javax.inject.Inject

class CheckInviteCodeValidUseCase
@Inject
constructor() {
    suspend operator fun invoke(): InviteCodeResult {
        // Todo : 검증 및 에러처리도 추후 추가 예정
        delay(100)
        return InviteCodeResult(
            isSuccess = true,
            errorMessage = null,
        )
    }
}
