package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.core.util.jvm.extension.isKorean
import com.teamyg.parfait.domain.model.NickNameResult
import javax.inject.Inject

class CheckNameValidUseCase
@Inject
constructor() {
    operator fun invoke(nickName: String): NickNameResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(nickName).not()) {
                return NickNameResult(
                    isSuccess = false,
                    errorMessage = validation.errorMessage,
                )
            }
        }

        return NickNameResult(
            isSuccess = true,
            errorMessage = null,
        )
    }
}

private enum class NameValidation(
    val isValid: (String) -> Boolean,
    val errorMessage: String,
) {
    CheckSpaceStartOrEnd(
        isValid = { nickName ->
            nickName.startsWith(" ").not() && nickName.endsWith(" ").not()
        },
        errorMessage = "닉네임의 처음과 끝에는 공백을 사용할 수 없어요",
    ),

    CheckDuplicatedSpace(
        isValid = { nickName ->
            nickName.indexOf("  ") == -1
        },
        errorMessage = "공백은 글자 사이에 1칸만 사용할 수 있어요",
    ),

    CheckValidCharacter(
        isValid = { nickName ->
            nickName.all { it.isWhitespace() || it.isDigit() || it.isLetter() || it.isKorean() }
        },
        errorMessage = "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요",
    ),
}
