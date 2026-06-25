package com.teamyg.parfait.feature.groupenter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupNickNameUiState(val dummy: String = "") : UiState {
}

sealed interface GroupNickNameIntent : UiIntent {
}

sealed interface GroupNickNameSideEffect : UiSideEffect {
}

@HiltViewModel
class GroupNickNameViewModel
@Inject
constructor(
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(),
) {
    override fun processIntent(intent: GroupNickNameIntent) {
    }
}
