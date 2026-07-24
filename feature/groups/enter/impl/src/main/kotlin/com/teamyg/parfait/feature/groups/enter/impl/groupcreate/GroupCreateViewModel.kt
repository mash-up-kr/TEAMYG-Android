package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import com.teamyg.parfait.core.ui.R as CoreR

data class GroupCreateUiState(
    val groupName: String = "",
    val nickName: String = "",
    val groupNumber: Int? = null,
    val groupNameErrorTextResId: Int? = null,
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
                        groupNameErrorTextResId = null,
                    )
                }
            }

            GroupCreateIntent.ClickNextButton -> {
                val result = checkNameValid(state.value.groupName)
                when (result) {
                    NameValidResult.Success -> {
                        updateState {
                            copy(groupNameErrorTextResId = null)
                        }
                        postSideEffect(GroupCreateSideEffect.NavigateToNext)
                    }

                    NameValidResult.Error.DuplicatedSpace -> {
                        updateState {
                            copy(groupNameErrorTextResId = CoreR.string.error_duplicated_space)
                        }
                    }

                    NameValidResult.Error.InvalidCharacter -> {
                        updateState {
                            copy(groupNameErrorTextResId = CoreR.string.error_invalid_character)
                        }
                    }

                    NameValidResult.Error.SpaceAtEdge -> {
                        updateState {
                            copy(groupNameErrorTextResId = CoreR.string.error_space_at_edge_groupname)
                        }
                    }

                    NameValidResult.Error.EmptyString -> {
                        updateState {
                            copy(groupNameErrorTextResId = CoreR.string.error_empty_space_groupname)
                        }
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
