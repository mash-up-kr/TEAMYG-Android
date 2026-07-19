package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.usecase.group.CheckInviteCodeValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInviteCodeUiState(
    val text: String = "",
    val focusedIndex: Int? = null,
    val inputMode: InputMode = InputMode.ADD,
    val errorText: String? = null,
) : UiState {
    val codeLength = 5
}

enum class InputMode {
    ADD,
    EDIT,
}

sealed interface GroupInviteCodeIntent : UiIntent {
    data object ClickNextButton : GroupInviteCodeIntent

    data object ClickBackButton : GroupInviteCodeIntent

    data class InputWord(val index: Int, val word: String) : GroupInviteCodeIntent

    data class SelectedTextFieldElement(val index: Int) : GroupInviteCodeIntent

    data object HideKeyboard : GroupInviteCodeIntent
}

sealed interface GroupInviteCodeSideEffect : UiSideEffect {
    data object NavigateToBack : GroupInviteCodeSideEffect

    data object NavigateToNext : GroupInviteCodeSideEffect
}

@HiltViewModel
class GroupInviteCodeViewModel
@Inject
constructor(
    private val checkInviteCodeValidUseCase: CheckInviteCodeValidUseCase,
) : BaseViewModel<GroupInviteCodeUiState, GroupInviteCodeIntent, GroupInviteCodeSideEffect>(
    initialState = GroupInviteCodeUiState(),
) {
    override fun processIntent(intent: GroupInviteCodeIntent) {
        when (intent) {
            GroupInviteCodeIntent.ClickBackButton -> postSideEffect(GroupInviteCodeSideEffect.NavigateToBack)

            GroupInviteCodeIntent.ClickNextButton -> {
                viewModelScope.launch {
                    val result = checkInviteCodeValidUseCase()
                    if (result.isSuccess) {
                        postSideEffect(GroupInviteCodeSideEffect.NavigateToNext)
                    } else {
                        updateState {
                            GroupInviteCodeUiState(
                                errorText = result.errorMessage,
                            )
                        }
                    }
                }
            }

            is GroupInviteCodeIntent.InputWord -> {
                updateState {
                    val addedWord = when (inputMode) {
                        InputMode.ADD -> intent.word.trim()
                        InputMode.EDIT -> intent.word.drop(1).trim()
                    }
                    val newFocusedIndex = intent.index.plus(addedWord.length).takeIf { it < codeLength }
                    val newText = (text.take(intent.index) + addedWord).take(codeLength)
                    if (text == newText) {
                        return@updateState this
                    }

                    when (inputMode) {
                        InputMode.ADD -> {
                            copy(
                                text = newText,
                                focusedIndex = newFocusedIndex,
                                inputMode = InputMode.ADD,
                                errorText = null,
                            )
                        }

                        InputMode.EDIT -> {
                            copy(
                                text = newText,
                                focusedIndex = newFocusedIndex,
                                inputMode = if (newFocusedIndex == newText.length) InputMode.ADD else InputMode.EDIT,
                                errorText = null,
                            )
                        }
                    }
                }
            }

            is GroupInviteCodeIntent.SelectedTextFieldElement -> {
                updateState {
                    val focusedIndex = intent.index.coerceAtMost(text.trim().length)
                    copy(
                        focusedIndex = focusedIndex,
                        inputMode = if (focusedIndex == text.trim().length) InputMode.ADD else InputMode.EDIT,
                    )
                }
            }

            is GroupInviteCodeIntent.HideKeyboard -> {
                updateState { copy(focusedIndex = null) }
            }
        }
    }
}
