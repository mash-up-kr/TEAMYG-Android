package com.teamyg.parfait.feature.groups.enter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.usecase.group.CheckNameValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupNickNameUiState(
    val nickName: String = "",
    val errorMessage: String? = null,
) : UiState

sealed interface GroupNickNameIntent : UiIntent {
    data class ClickNextButton(val nickName: String) : GroupNickNameIntent

    data object ClickBackButton : GroupNickNameIntent

    data class InputWord(val nickName: String) : GroupNickNameIntent
}

sealed interface GroupNickNameSideEffect : UiSideEffect {
    data object NavigateToBack : GroupNickNameSideEffect

    data object NavigateToNext : GroupNickNameSideEffect
}

@HiltViewModel
class GroupNickNameViewModel
@Inject
constructor(
    private val checkNickNameValid: CheckNameValidUseCase,
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(),
) {
    override fun processIntent(intent: GroupNickNameIntent) {
        when (intent) {
            GroupNickNameIntent.ClickBackButton -> postSideEffect(GroupNickNameSideEffect.NavigateToBack)

            is GroupNickNameIntent.ClickNextButton -> {
                val result = checkNickNameValid(intent.nickName)
                if (result.isSuccess) {
                    updateState {
                        copy(errorMessage = null)
                    }
                    postSideEffect(GroupNickNameSideEffect.NavigateToNext)
                } else {
                    updateState {
                        copy(errorMessage = result.errorMessage)
                    }
                }
            }

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(nickName = intent.nickName)
                }
            }
        }
    }
}
