package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.CreateGroupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch

data class GroupCreateUiState(
    val groupName: String = "",
    val nickName: String = "",
    val groupNumber: Int? = null,
    val groupNameError: NameValidResult.Error? = null,
    val isConfirmPopupVisible: Boolean = false,
    val isCreating: Boolean = false,
) : UiState {
    val isValid = groupName.isNotEmpty() && nickName.isNotEmpty() && groupNumber != null
}

sealed interface GroupCreateIntent : UiIntent {
    data object ClickNextButton : GroupCreateIntent

    data object ClickBackButton : GroupCreateIntent

    data class InputGroupName(val newGroupName: String) : GroupCreateIntent

    data class ClickGroupNumber(val newSelectedNumber: Int) : GroupCreateIntent

    data object ClickConfirmPopupCreate : GroupCreateIntent

    data object DismissConfirmPopup : GroupCreateIntent
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
    private val createGroup: CreateGroupUseCase,
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
                        groupNameError = null,
                    )
                }
            }

            GroupCreateIntent.ClickNextButton -> {
                when (val result = checkNameValid(state.value.groupName)) {
                    NameValidResult.Success -> {
                        updateState {
                            copy(
                                groupNameError = null,
                                isConfirmPopupVisible = true,
                            )
                        }
                    }

                    is NameValidResult.Error -> {
                        updateState {
                            copy(groupNameError = result)
                        }
                    }
                }
            }

            GroupCreateIntent.DismissConfirmPopup -> {
                if (state.value.isCreating) return

                updateState { copy(isConfirmPopupVisible = false) }
            }

            GroupCreateIntent.ClickConfirmPopupCreate -> {
                val groupNumber = state.value.groupNumber ?: return
                if (state.value.isCreating) return

                updateState { copy(isCreating = true) }
                viewModelScope.launch {
                    val result = createGroup(
                        groupName = state.value.groupName,
                        groupNumber = groupNumber,
                    )
                    updateState { copy(isCreating = false) }

                    // Todo : 실패 처리는 서버 작업이 연결되면 추가 예정입니다
                    if (result.isSuccess) {
                        updateState { copy(isConfirmPopupVisible = false) }
                        postSideEffect(GroupCreateSideEffect.NavigateToNext)
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
