package com.teamyg.parfait.domain.usecase.group

import com.teamyg.parfait.domain.model.NickNameResult
import javax.inject.Inject

class CheckNickNameValidUseCase
@Inject
constructor() {
    operator fun invoke(nickName: String): NickNameResult {
        NameValidation.entries.forEach { validation ->
            if (validation.isValid(nickName).not()) {
                return NickNameResult(
                    isSuccess = false,
                    errorMessage = mapErrorMessage(validation),
                )
            }
        }

        return NickNameResult(
            isSuccess = true,
            errorMessage = null,
        )
    }

    private fun mapErrorMessage(validation: NameValidation): String = when (validation) {
        NameValidation.CheckSpaceStartOrEnd -> "닉네임의 처음과 끝에는 공백을 사용할 수 없어요"
        NameValidation.CheckDuplicatedSpace -> "공백은 글자 사이에 1칸만 사용할 수 있어요"
        NameValidation.CheckValidCharacter -> "한글, 영문, 숫자, 띄어쓰기만 사용할 수 있어요"
    }
}
