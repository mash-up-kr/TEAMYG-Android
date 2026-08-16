package com.teamyg.parfait.feature.groups.enter.impl.invitecode

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.usecase.group.GetGroupJoinPreviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupInviteCodeUiState(
    val text: String = "",
    val focusedIndex: Int? = null,
    val inputMode: InputMode = InputMode.ADD,
    val inviteCodeError: InviteCodeError? = null,
    val isSubmitting: Boolean = false,
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

    data class ClipboardCodeDetected(val code: String?) : GroupInviteCodeIntent

    data object ClickPasteInviteCode : GroupInviteCodeIntent
}

sealed interface GroupInviteCodeSideEffect : UiSideEffect {
    data object NavigateToBack : GroupInviteCodeSideEffect

    data class NavigateToNext(
        val inviteCode: String,
        val groupName: String,
    ) : GroupInviteCodeSideEffect
}

@HiltViewModel
class GroupInviteCodeViewModel
@Inject
constructor(
    private val getGroupJoinPreview: GetGroupJoinPreviewUseCase,
) : BaseViewModel<GroupInviteCodeUiState, GroupInviteCodeIntent, GroupInviteCodeSideEffect>(
    initialState = GroupInviteCodeUiState(),
) {
    override fun processIntent(intent: GroupInviteCodeIntent) {
        when (intent) {
            GroupInviteCodeIntent.ClickBackButton -> postSideEffect(GroupInviteCodeSideEffect.NavigateToBack)

            GroupInviteCodeIntent.ClickNextButton -> requestJoinPreview()

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
                                inviteCodeError = null,
                            )
                        }

                        InputMode.EDIT -> {
                            copy(
                                text = newText,
                                focusedIndex = newFocusedIndex,
                                inputMode = if (newFocusedIndex == newText.length) InputMode.ADD else InputMode.EDIT,
                                inviteCodeError = null,
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
                        inviteCodeError = null,
                        clipboardInviteCode = null,
                    )
                }
            }
        }
    }

    private fun requestJoinPreview() {
        val inviteCode = state.value.text
        if (inviteCode.length != state.value.codeLength) {
            viewModelLogger.d { "초대코드가 ${state.value.codeLength}자가 아니라 조회하지 않는다" }
            return
        }

        launch(key = KEY_JOIN_PREVIEW) {
            updateState { copy(isSubmitting = true, inviteCodeError = null) }
            try {
                getGroupJoinPreview(InviteCode(inviteCode))
                    .onSuccess { groupName ->
                        postSideEffect(
                            GroupInviteCodeSideEffect.NavigateToNext(
                                inviteCode = inviteCode,
                                groupName = groupName.value,
                            ),
                        )
                    }.onFailure(::handleFailure)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
                updateState { copy(isSubmitting = false) }
            }
        }
    }

    /**
     * 실패 갈래를 전부 열거해 둔다. 화면에는 입력 자리 아래 한 줄로만 나가므로, 갈래마다
     * 문구를 고르고 원인은 로그로 남긴다.
     */
    private fun handleFailure(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> InviteCodeError.NETWORK

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.ParfaitGroup.INVALID_INVITE_CODE -> InviteCodeError.INVALID_CODE
                ServerErrorCode.ParfaitGroup.GROUP_ALREADY_JOINED -> InviteCodeError.ALREADY_JOINED
                ServerErrorCode.ParfaitGroup.GROUP_MEMBER_LIMIT_REACHED -> InviteCodeError.MEMBER_LIMIT_REACHED
                else -> InviteCodeError.UNKNOWN
            }

            else -> InviteCodeError.UNKNOWN
        }

        viewModelLogger.e(throwable) { "초대코드 조회 실패 — $error" }
        updateState { copy(inviteCodeError = error) }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 초대코드 조회 job 하나를 가리킨다 */
        const val KEY_JOIN_PREVIEW = "joinPreview"
    }
}
