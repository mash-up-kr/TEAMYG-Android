package com.teamyg.parfait.core.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.ui.R
import com.teamyg.parfait.domain.model.NickNameResult

@Composable
fun NickNameResult.Error.toStringResource(): String = when (this) {
    NickNameResult.Error.Empty -> stringResource(R.string.nickname_error_empty)
    NickNameResult.Error.SpaceAtEdge -> stringResource(R.string.nickname_error_space_at_edge)
    NickNameResult.Error.DuplicatedSpace -> stringResource(R.string.nickname_error_duplicated_space)
    NickNameResult.Error.InvalidCharacter -> stringResource(R.string.nickname_error_invalid_character)
}
