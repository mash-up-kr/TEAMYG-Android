package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.NicknameResult
import javax.inject.Inject

class CheckGroupNameValidUseCase
@Inject
constructor() {
    operator fun invoke(nickName: String): NicknameResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(nickName).not()) {
                return validation.errorType
            }
        }

        return NicknameResult.Success
    }
}
