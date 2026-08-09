package com.teamyg.parfait.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.core.ui.R as CoreR

/**
 * [NameValidResult.Error]가 어떤 대상(닉네임/그룹명)의 검증 결과인지 구분한다.
 *
 * [NameValidResult.Error.SpaceAtEdge]·[NameValidResult.Error.EmptyString]만 대상별로
 * 다른 문구를 쓰고, 나머지는 공용 문구를 쓴다.
 */
enum class NameFieldType {
    NICKNAME,
    GROUP_NAME,
}

/**
 * 도메인의 [NameValidResult.Error]를 화면에 표시할 문자열로 변환한다.
 *
 * 표시 문자열 매핑을 [com.teamyg.parfait.core.ui]가 단일 소유하고, ViewModel의 UI State는
 * 도메인 의미([NameValidResult.Error])만 들고 있도록 하기 위한 확장이다.
 */
@Composable
fun NameValidResult.Error.toStringResource(fieldType: NameFieldType): String = when (this) {
    NameValidResult.Error.DuplicatedSpace -> stringResource(CoreR.string.error_duplicated_space)

    NameValidResult.Error.InvalidCharacter -> stringResource(CoreR.string.error_invalid_character)

    NameValidResult.Error.SpaceAtEdge -> when (fieldType) {
        NameFieldType.NICKNAME -> stringResource(CoreR.string.error_space_at_edge_nickname)
        NameFieldType.GROUP_NAME -> stringResource(CoreR.string.error_space_at_edge_groupname)
    }

    NameValidResult.Error.EmptyString -> when (fieldType) {
        NameFieldType.NICKNAME -> stringResource(CoreR.string.error_empty_space_nickname)
        NameFieldType.GROUP_NAME -> stringResource(CoreR.string.error_empty_space_groupname)
    }
}
