package com.teamyg.parfait.feature.app.setting.impl.viewmodel

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.member.GlobalNickname
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.member.ChangeGlobalNicknameUseCase
import com.teamyg.parfait.domain.usecase.member.ObserveMyAccountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * @property savedNickname 계정 정보 SSoT 가 준 현재 값. `null` 은 아직 방출 전인 로딩
 *   상태다(빈 문자열이 아니다). [nickname] 과 나눠 두는 이유는 "되돌릴 것이 있는가"를
 *   판단해야 해서다 — 입력 버퍼 하나만으로는 사용자가 뭘 바꿨는지 알 수 없다.
 * @property nickname 입력 필드 값. 타이핑은 이쪽만 바꾼다.
 * @property nicknameError 입력 형식 위반(`CheckNameValidUseCase`). 요청 전에 걸러진다.
 * @property submitError 서버가 되돌린 사유. [nicknameError] 와 별개 축이라 형식 오류를
 *   먼저 보여준다(`nicknameError ?: submitError`, ADR-0016).
 * @property isSubmitting 변경 요청이 진행 중인지. 진행 중이면 확인 버튼을 비활성한다.
 * @property isEditing 입력 필드에 포커스가 있는지. 확인 버튼은 이때만 보인다.
 * @property isDiscardDialogVisible 수정 중 뒤로가기를 눌러 확인을 묻고 있는지.
 */
data class AccountInfoUiState(
    val savedNickname: String? = null,
    val nickname: String? = null,
    val nicknameError: NameValidResult.Error? = null,
    val submitError: GlobalNicknameError? = null,
    val isSubmitting: Boolean = false,
    val isEditing: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
) : UiState {
    /** 서버 값과 달라 되돌릴 것이 있는 상태 */
    val isDirty: Boolean
        get() = nickname != null && nickname != savedNickname

    val isConfirmEnabled: Boolean
        get() = isDirty && nicknameError == null && isSubmitting.not()
}

sealed interface AccountInfoIntent : UiIntent {
    data class InputWord(val nickName: String) : AccountInfoIntent

    data class ChangeFocus(val hasFocus: Boolean) : AccountInfoIntent

    data object ClickConfirm : AccountInfoIntent

    data object ClickBack : AccountInfoIntent

    /** 수정 취소 확인의 "그만두기" — 입력을 버리고 나간다 */
    data object ConfirmDiscard : AccountInfoIntent

    /** 수정 취소 확인의 "취소하기" — 다이얼로그만 닫고 편집을 이어간다 */
    data object DismissDiscardDialog : AccountInfoIntent
}

sealed interface AccountInfoSideEffect : UiSideEffect {
    data object NavigateBack : AccountInfoSideEffect
}

@HiltViewModel
class AccountInfoViewModel
@Inject
constructor(
    private val observeMyAccount: ObserveMyAccountUseCase,
    private val checkNameValid: CheckNameValidUseCase,
    private val changeGlobalNickname: ChangeGlobalNicknameUseCase,
) : BaseViewModel<AccountInfoUiState, AccountInfoIntent, AccountInfoSideEffect>(
    initialState = AccountInfoUiState(),
) {
    init {
        viewModelLogger.i { "AccountInfoViewModel::init" }

        // 구독만 한다 — 변경 성공 후의 새 값도 이 구독이 되돌려준다(낙관적 갱신 안 함).
        // 입력 버퍼까지 같이 따라가는 이유: SSoT 는 저장된 값이 실제로 달라질 때만 방출하므로
        // (로컬 저장소가 원문 기준으로 중복 방출을 끊는다) 타이핑 도중에 끼어들지 않는다.
        launch {
            observeMyAccount().collect { account ->
                val serverNickname = account?.nickname?.value
                updateState { copy(savedNickname = serverNickname, nickname = serverNickname) }
            }
        }
    }

    override fun processIntent(intent: AccountInfoIntent) {
        when (intent) {
            is AccountInfoIntent.InputWord -> handleInputWord(intent.nickName)
            is AccountInfoIntent.ChangeFocus -> handleChangeFocus(intent.hasFocus)
            AccountInfoIntent.ClickConfirm -> handleClickConfirm()
            AccountInfoIntent.ClickBack -> handleClickBack()
            AccountInfoIntent.ConfirmDiscard -> handleConfirmDiscard()
            AccountInfoIntent.DismissDiscardDialog -> handleDismissDiscardDialog()
        }
    }

    private fun handleChangeFocus(hasFocus: Boolean) {
        updateState { copy(isEditing = hasFocus) }
    }

    private fun handleInputWord(nickName: String) {
        val nicknameError = checkNameValid(nickName) as? NameValidResult.Error

        updateState {
            copy(
                nickname = nickName,
                nicknameError = nicknameError,
                submitError = null,
            )
        }
    }

    /**
     * 되돌릴 것이 있을 때만 묻는다 — 구경만 하고 나가는 사용자를 막지 않는다.
     */
    private fun handleClickBack() {
        if (state.value.isDirty) {
            updateState { copy(isDiscardDialogVisible = true) }
        } else {
            postSideEffect(AccountInfoSideEffect.NavigateBack)
        }
    }

    private fun handleConfirmDiscard() {
        updateState {
            copy(
                nickname = savedNickname,
                nicknameError = null,
                submitError = null,
                isDiscardDialogVisible = false,
            )
        }
        postSideEffect(AccountInfoSideEffect.NavigateBack)
    }

    private fun handleDismissDiscardDialog() {
        updateState { copy(isDiscardDialogVisible = false) }
    }

    private fun handleClickConfirm() {
        val nickname = state.value.nickname ?: return

        when (val result = checkNameValid(nickname)) {
            is NameValidResult.Error -> {
                updateState { copy(nicknameError = result) }
                return
            }

            NameValidResult.Success -> Unit
        }

        launch(key = KEY_CHANGE_NICKNAME) {
            updateState {
                copy(
                    nicknameError = null,
                    submitError = null,
                    isSubmitting = true,
                )
            }
            try {
                // 성공해도 여기서 `nickname` 을 쓰지 않는다 — 새 값은 위 구독이 SSoT 로부터
                // 되돌려준다. 직접 쓰면 SSoT 저장이 실패해도 화면만 낙관적으로 바뀐 상태가 된다.
                changeGlobalNickname(GlobalNickname(nickname)).onFailure(::handleFailure)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
                updateState { copy(isSubmitting = false) }
            }
        }
    }

    /** 실패 갈래를 전부 열거해 둔다. 화면에는 입력 자리 아래 한 줄로만 나간다 */
    private fun handleFailure(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> GlobalNicknameError.NETWORK

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.Member.INVALID_NICKNAME -> GlobalNicknameError.INVALID
                else -> GlobalNicknameError.UNKNOWN
            }

            else -> GlobalNicknameError.UNKNOWN
        }

        viewModelLogger.e(throwable) { "전역 닉네임 변경 실패 — $error" }
        updateState { copy(submitError = error) }
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 닉네임 변경 job 하나를 가리킨다 */
        const val KEY_CHANGE_NICKNAME = "changeGlobalNickname"
    }
}
