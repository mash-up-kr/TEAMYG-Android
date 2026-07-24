package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.core.util.jvm.extension.isKorean
import com.teamyg.parfait.domain.model.NameValidResult
import javax.inject.Inject

class CheckNameValidUseCase
@Inject
constructor() {
    operator fun invoke(nickName: String): NameValidResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(nickName).not()) {
                return validation.errorType
            }
        }

        return NameValidResult.Success
    }
}

private enum class NameValidation(
    val isValid: (String) -> Boolean,
    val errorType: NameValidResult.Error,
) {
    CheckSpaceStartOrEnd(
        isValid = { nickName ->
            nickName.startsWith(" ").not() && nickName.endsWith(" ").not()
        },
        errorType = NameValidResult.Error.SpaceAtEdge,
    ),

    CheckDuplicatedSpace(
        isValid = { nickName ->
            nickName.indexOf("  ") == -1
        },
        errorType = NameValidResult.Error.DuplicatedSpace,
    ),

    CheckValidCharacter(
        isValid = { nickName ->
            nickName.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        errorType = NameValidResult.Error.InvalidCharacter,
    ),

    CheckEmptyString(
        isValid = { nickName ->
            nickName.isNotEmpty()
        },
        errorType = NameValidResult.Error.EmptyString,
    ),
}
