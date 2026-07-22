package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.core.util.jvm.extension.isKorean
import com.teamyg.parfait.domain.model.NicknameResult
import javax.inject.Inject

class CheckNameValidUseCase
@Inject
constructor() {
    operator fun invoke(nickName: String): NicknameResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(nickName).not()) {
                return validation.error
            }
        }

        return NicknameResult.Success
    }
}

private enum class NameValidation(
    val isValid: (String) -> Boolean,
    val error: NicknameResult.Error,
) {
    CheckEmpty(
        isValid = { nickname ->
            nickname.isNotEmpty()
        },
        error = NicknameResult.Error.Empty,
    ),

    CheckSpaceStartOrEnd(
        isValid = { nickname ->
            nickname.startsWith(" ").not() && nickname.endsWith(" ").not()
        },
        error = NicknameResult.Error.SpaceAtEdge,
    ),

    CheckDuplicatedSpace(
        isValid = { nickname ->
            nickname.indexOf("  ") == -1
        },
        error = NicknameResult.Error.DuplicatedSpace,
    ),

    CheckValidCharacter(
        isValid = { nickname ->
            nickname.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        error = NicknameResult.Error.InvalidCharacter,
    ),
}
