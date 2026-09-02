package com.teamyg.parfait.domain.usecase

import com.teamyg.parfait.domain.model.NameValidResult
import javax.inject.Inject

/**
 * 그룹명·닉네임 유효성을 입력 중에 즉시 알려준다. 최종 판정은 서버가 한다.
 *
 * 규칙은 서버 정규식 `^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+(?: [가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9]+)*$` 와 같은 집합으로
 * 유지한다 — 서버보다 느슨하면 여기를 통과한 이름이 서버에서만 튕겨 필드에 아무 표시도 남지 않고,
 * 좁으면 서버가 받는 이름을 앱이 먼저 막는다.
 */
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

    /**
     * `isLetter()`·`isDigit()`·`isWhitespace()` 는 유니코드 전체를 받아 서버보다 넓어진다 —
     * 일본어·아랍 숫자·non-breaking space 가 통과한 뒤 서버에서만 400 으로 튕긴다.
     * 자모 단독(`ㅋㅋ`·`ㅠㅠ`)은 2026-08-15 서버 변경으로 허용 대상이 됐다.
     */
    CheckValidCharacter(
        isValid = { name ->
            name.all { char ->
                char == ' ' ||
                    char in '가'..'힣' ||
                    char in 'ㄱ'..'ㅎ' ||
                    char in 'ㅏ'..'ㅣ' ||
                    char in 'A'..'Z' ||
                    char in 'a'..'z' ||
                    char in '0'..'9'
            }
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
