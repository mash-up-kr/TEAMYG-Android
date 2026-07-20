package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.usecase.group.CheckNameValidUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class GroupCreateUiState(
    val groupName: String = "",
    val nickName: String = "",
    val groupNumber: Int? = null,
    val groupNameErrorText: String? = null,
) : UiState {
    val isValid = groupName.isNotEmpty() && nickName.isNotEmpty() && groupNumber != null
}

sealed interface GroupCreateIntent : UiIntent {
    data object ClickNextButton : GroupCreateIntent

    data object ClickBackButton : GroupCreateIntent

    data class InputGroupName(val newGroupName: String) : GroupCreateIntent

    data class ClickGroupNumber(val newSelectedNumber: Int) : GroupCreateIntent
}

sealed interface GroupCreateSideEffect : UiSideEffect {
    data object NavigateToBack : GroupCreateSideEffect

    data object NavigateToNext : GroupCreateSideEffect
}

@HiltViewModel(assistedFactory = GroupCreateViewModel.Factory::class)
class GroupCreateViewModel
@AssistedInject
constructor(
    @Assisted nickName: String,
    private val checkNameValid: CheckNameValidUseCase,
) : BaseViewModel<GroupCreateUiState, GroupCreateIntent, GroupCreateSideEffect>(
    initialState = GroupCreateUiState(nickName = nickName),
) {
    override fun processIntent(intent: GroupCreateIntent) {
        when (intent) {
            GroupCreateIntent.ClickBackButton -> postSideEffect(GroupCreateSideEffect.NavigateToBack)

            is GroupCreateIntent.ClickGroupNumber -> {
                updateState { copy(groupNumber = intent.newSelectedNumber) }
            }

            is GroupCreateIntent.InputGroupName -> {
                updateState {
                    copy(
                        groupName = intent.newGroupName,
                        groupNameErrorText = null,
                    )
                }
            }

            GroupCreateIntent.ClickNextButton -> {
                val result = checkNameValid(state.value.groupName)
                if (result.isSuccess) {
                    updateState {
                        copy(groupNameErrorText = null)
                    }
                    postSideEffect(GroupCreateSideEffect.NavigateToNext)
                } else {
                    updateState {
                        copy(groupNameErrorText = result.errorMessage)
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(nickName: String): GroupCreateViewModel
    }
}
