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
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupInviteCodeUiState(
    val text: String = "",
    /** 밑줄을 칠 칸(0..[codeLength]-1) — 입력한 글자 수만큼 뒤로 가되 마지막 칸에서 멈춘다 */
    val focusedIndex: Int = 0,
    val isFocused: Boolean = false,
    val inviteCodeError: InviteCodeError? = null,
    val isSubmitting: Boolean = false,
    val clipboardInviteCode: String? = null,
    val nickName: String? = null,
) : UiState {
    val codeLength = InviteCode.LENGTH

    /**
     * 텍스트 필드에 넘길 커서 자리.
     *
     * 지우기는 커서 앞 글자를 지운다. 포커스 칸이 차 있으면 그 뒤, 비어 있으면 그 자리에 두어
     * "찬 칸은 그 칸이, 빈 칸은 앞 칸이" 지워지게 한다.
     */
    val cursor: Int
        get() = if (focusedIndex < text.length) focusedIndex + 1 else focusedIndex

    /**
     * 붙여넣기 바에 노출할 초대코드.
     *
     * 클립보드에서 초대코드를 찾았고, 키보드가 올라와 있으며,
     * 아직 그 코드가 입력되지 않았을 때만 값이 있다.
     */
    val pasteBarInviteCode: String?
        get() = clipboardInviteCode?.takeIf { code -> isFocused && code != text }
}

sealed interface GroupInviteCodeIntent : UiIntent {
    data object ClickNextButton : GroupInviteCodeIntent

    data object ClickBackButton : GroupInviteCodeIntent

    /** 추가·삭제·붙여넣기가 모두 이 인텐트 하나로 들어온다 */
    data class ChangeText(val text: String, val cursor: Int) : GroupInviteCodeIntent

    data class SelectedTextFieldElement(val index: Int) : GroupInviteCodeIntent

    data object HideKeyboard : GroupInviteCodeIntent

    data object RequestFocus : GroupInviteCodeIntent

    /** 칸 사이 여백을 누르면 필드가 스스로 포커스를 가져가므로, 그 사실을 화면이 따라가야 한다 */
    data class FocusChanged(val isFocused: Boolean) : GroupInviteCodeIntent

    data class ClipboardCodeDetected(val code: String?) : GroupInviteCodeIntent

    data object ClickPasteInviteCode : GroupInviteCodeIntent
}

sealed interface GroupInviteCodeSideEffect : UiSideEffect {
    data object NavigateToBack : GroupInviteCodeSideEffect

    data class NavigateToNext(
        val inviteCode: String,
        val groupName: String,
        val nickName: String,
    ) : GroupInviteCodeSideEffect
}

@HiltViewModel
class GroupInviteCodeViewModel
@Inject
constructor(
    private val getGroupJoinPreview: GetGroupJoinPreviewUseCase,
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
) : BaseViewModel<GroupInviteCodeUiState, GroupInviteCodeIntent, GroupInviteCodeSideEffect>(
    initialState = GroupInviteCodeUiState(),
) {
    init {
        observeMyAccount()
    }

    /** 그룹 내 닉네임의 초기값은 계정 공통 값을 재사용한다 — 그룹마다 새로 뽑지 않는다 */
    private fun observeMyAccount() {
        launch {
            getMyAccountFlow().collect { account ->
                updateState { copy(nickName = account?.nickname?.value) }
            }
        }
    }

    override fun processIntent(intent: GroupInviteCodeIntent) {
        when (intent) {
            GroupInviteCodeIntent.ClickBackButton -> postSideEffect(GroupInviteCodeSideEffect.NavigateToBack)

            GroupInviteCodeIntent.ClickNextButton -> {
                updateState { copy(isFocused = false) }
                requestJoinPreview()
            }

            // 커서를 기준으로 앞뒤를 나눠 각각 거른 뒤 다시 붙인다. 통째로 거르면 걸러진 글자가
            // 커서 앞이었는지 뒤였는지를 잃어 포커스가 엉뚱한 칸으로 간다.
            is GroupInviteCodeIntent.ChangeText -> {
                updateState {
                    val cursor = intent.cursor.coerceIn(0, intent.text.length)
                    val head = intent.text
                        .take(cursor)
                        .filter(InviteCode::isCodeChar)
                        .take(codeLength)
                    val tail = intent.text
                        .drop(cursor)
                        .filter(InviteCode::isCodeChar)
                    val newText = (head + tail).take(codeLength)
                    copy(
                        text = newText,
                        focusedIndex = newText.focusedIndex(),
                        // 코드가 실제로 안 바뀌면 사유도 남긴다 — 문구가 사라지며 화면이 튀지 않게
                        inviteCodeError = inviteCodeError.takeIf { newText == text },
                    )
                }
            }

            // 빈 칸을 눌러도 글자 끝까지만 간다 — 중간에 빈칸이 생기지 않는다
            is GroupInviteCodeIntent.SelectedTextFieldElement -> {
                updateState {
                    copy(focusedIndex = intent.index.coerceIn(0, text.focusedIndex()), isFocused = true)
                }
            }

            is GroupInviteCodeIntent.HideKeyboard -> {
                updateState { copy(isFocused = false) }
            }

            is GroupInviteCodeIntent.RequestFocus -> {
                updateState { copy(focusedIndex = 0, isFocused = true) }
            }

            is GroupInviteCodeIntent.FocusChanged -> {
                updateState { copy(isFocused = intent.isFocused) }
            }

            is GroupInviteCodeIntent.ClipboardCodeDetected -> {
                updateState { copy(clipboardInviteCode = intent.code) }
            }

            GroupInviteCodeIntent.ClickPasteInviteCode -> {
                updateState {
                    val pastedCode = clipboardInviteCode ?: return@updateState this
                    val newText = pastedCode.take(codeLength)
                    // 코드가 모두 채워지므로 포커스를 놓아 키보드를 내린다
                    copy(
                        text = newText,
                        focusedIndex = newText.focusedIndex(),
                        isFocused = false,
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
                        // 닉네임이 없다고 넘어가는 것까지 막지는 않는다 — 조회를 이미 마친
                        // 뒤라 여기서 되돌리면 사용자는 아무 반응 없는 실패로 본다
                        val nickName = state.value.nickName
                        if (nickName == null) {
                            viewModelLogger.w { "앱 닉네임이 아직 없어 닉네임 초기값 없이 넘어간다" }
                        }

                        postSideEffect(
                            GroupInviteCodeSideEffect.NavigateToNext(
                                inviteCode = inviteCode,
                                groupName = groupName.value,
                                nickName = nickName.orEmpty(),
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

    private fun String.focusedIndex(): Int = length.coerceAtMost(InviteCode.LENGTH - 1)

    private companion object {
        /** [launch] 중복 실행 가드 키 — 초대코드 조회 job 하나를 가리킨다 */
        const val KEY_JOIN_PREVIEW = "joinPreview"
    }
}
