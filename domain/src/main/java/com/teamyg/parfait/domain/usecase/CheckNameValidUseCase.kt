package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.core.util.jvm.extension.isKorean
import com.teamyg.parfait.domain.model.NameValidResult
import javax.inject.Inject

class CheckNameValidUseCase
@Inject
constructor() {
    operator fun invoke(name: String): NameValidResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(name).not()) {
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
        isValid = { name ->
            name.startsWith(" ").not() && name.endsWith(" ").not()
        },
        errorType = NameValidResult.Error.SpaceAtEdge,
    ),

    CheckDuplicatedSpace(
        isValid = { name ->
            name.indexOf("  ") == -1
        },
        errorType = NameValidResult.Error.DuplicatedSpace,
    ),

    CheckValidCharacter(
        isValid = { name ->
            name.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        errorType = NameValidResult.Error.InvalidCharacter,
    ),

    CheckEmptyString(
        isValid = { name ->
            name.isNotEmpty()
        },
        errorType = NameValidResult.Error.EmptyString,
    ),
}
