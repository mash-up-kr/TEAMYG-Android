package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.core.util.jvm.extension.isKorean
import com.teamyg.parfait.domain.model.NicknameResult

enum class NameValidation(val isValid: (String) -> Boolean, val errorType: NicknameResult.Error) {
    CheckSpaceStartOrEnd(
        isValid = { nickName ->
            nickName.startsWith(" ").not() && nickName.endsWith(" ").not()
        },
        errorType = NicknameResult.Error.SpaceAtEdge,
    ),

    CheckDuplicatedSpace(
        isValid = { nickName ->
            nickName.indexOf("  ") == -1
        },
        errorType = NicknameResult.Error.DuplicatedSpace,
    ),

    CheckValidCharacter(
        isValid = { nickName ->
            nickName.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        errorType = NicknameResult.Error.InvalidCharacter,
    ),
}
