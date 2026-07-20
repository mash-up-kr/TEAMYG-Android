package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupCreateUiState(
    val groupName: String = "",
    val nickName: String = "",
    val groupNumber: Int? = null,
) : UiState {
    val isValid = groupName.isEmpty() && nickName.isNotEmpty() && groupNumber != null
}

sealed interface GroupCreateIntent : UiIntent {
    data object ClickNextButton : GroupCreateIntent

    data object ClickBackButton : GroupCreateIntent

    data object HideKeyboard : GroupCreateIntent
}

sealed interface GroupCreateSideEffect : UiSideEffect {
    data object NavigateToBack : GroupCreateSideEffect

    data object NavigateToNext : GroupCreateSideEffect
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
