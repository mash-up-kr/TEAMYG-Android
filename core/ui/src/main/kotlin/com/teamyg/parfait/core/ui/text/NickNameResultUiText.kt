package com.teamyg.parfait.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.ui.R
import com.teamyg.parfait.domain.model.NicknameResult

@Composable
fun NicknameResult.Error.toStringResource(): String = when (this) {
    NicknameResult.Error.Empty -> stringResource(R.string.nickname_error_empty)
    NicknameResult.Error.SpaceAtEdge -> stringResource(R.string.nickname_error_space_at_edge)
    NicknameResult.Error.DuplicatedSpace -> stringResource(R.string.nickname_error_duplicated_space)
    NicknameResult.Error.InvalidCharacter -> stringResource(R.string.nickname_error_invalid_character)
}
