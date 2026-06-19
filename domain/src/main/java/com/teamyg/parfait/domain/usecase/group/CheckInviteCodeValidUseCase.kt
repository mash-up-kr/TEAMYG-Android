package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.InviteCodeResult
import kotlinx.coroutines.delay
import javax.inject.Inject

class CheckInviteCodeValidUseCase
@Inject
constructor() {
    suspend operator fun invoke(): InviteCodeResult {
        delay(100)
        return InviteCodeResult(
            isSuccess = true,
            errorMessage = null,
        )
    }
}
