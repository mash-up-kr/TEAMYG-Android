package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupCreateUiState(
    val text: String = "",
) : UiState
sealed interface GroupCreateIntent : UiIntent {
}

sealed interface GroupCreateSideEffect : UiSideEffect {
}

@HiltViewModel
class GroupCreateViewModel
@Inject
constructor(
) : BaseViewModel<GroupCreateUiState, GroupCreateIntent, GroupCreateSideEffect>(
    initialState = GroupCreateUiState(),
) {
    override fun processIntent(intent: GroupCreateIntent) {
    }
}
