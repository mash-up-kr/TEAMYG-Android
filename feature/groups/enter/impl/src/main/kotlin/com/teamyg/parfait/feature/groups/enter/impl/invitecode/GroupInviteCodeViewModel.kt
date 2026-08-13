package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.usecase.group.CheckInviteCodeValidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupInviteCodeUiState(
    val text: String = "",
    val focusedIndex: Int? = null,
    val inputMode: InputMode = InputMode.ADD,
    val errorText: String? = null,
    val groupName: String = "",
    val isConfirmPopupVisible: Boolean = false,
    val clipboardInviteCode: String? = null,
) : UiState {
    val codeLength = InviteCode.LENGTH

    /**
     * 붙여넣기 바에 노출할 초대코드.
     *
     * 클립보드에서 초대코드를 찾았고, 키보드가 올라와 있으며,
     * 아직 그 코드가 입력되지 않았을 때만 값이 있다.
     */
    val pasteBarInviteCode: String?
        get() = clipboardInviteCode?.takeIf { code -> focusedIndex != null && code != text }
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

    data object FocusedFirstIndex : GroupInviteCodeIntent

    data object ClickConfirmPopupEnter : GroupInviteCodeIntent

    data object DismissConfirmPopup : GroupInviteCodeIntent

    data class ClipboardCodeDetected(val code: String?) : GroupInviteCodeIntent

    data object ClickPasteInviteCode : GroupInviteCodeIntent
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
                        updateState {
                            copy(
                                groupName = result.groupName,
                                isConfirmPopupVisible = true,
                            )
                        }
                    } else {
                        updateState {
                            GroupInviteCodeUiState(
                                errorText = result.errorMessage,
                                clipboardInviteCode = clipboardInviteCode,
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

            is GroupInviteCodeIntent.FocusedFirstIndex -> {
                updateState { copy(focusedIndex = 0) }
            }

            GroupInviteCodeIntent.DismissConfirmPopup -> {
                updateState { copy(isConfirmPopupVisible = false) }
            }

            GroupInviteCodeIntent.ClickConfirmPopupEnter -> {
                updateState { copy(isConfirmPopupVisible = false) }
                postSideEffect(GroupInviteCodeSideEffect.NavigateToNext)
            }

            is GroupInviteCodeIntent.ClipboardCodeDetected -> {
                updateState { copy(clipboardInviteCode = intent.code) }
            }

            GroupInviteCodeIntent.ClickPasteInviteCode -> {
                updateState {
                    val pastedCode = clipboardInviteCode ?: return@updateState this
                    // 코드가 모두 채워지므로 focusedIndex 를 비워 키보드를 내린다
                    copy(
                        text = pastedCode.take(codeLength),
                        focusedIndex = null,
                        inputMode = InputMode.ADD,
                        errorText = null,
                        clipboardInviteCode = null,
                    )
                }
            }
        }
    }
}
