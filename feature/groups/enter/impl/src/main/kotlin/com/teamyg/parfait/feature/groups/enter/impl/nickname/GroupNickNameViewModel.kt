package com.teamyg.parfait.feature.groups.enter.impl.nickname

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import com.teamyg.parfait.domain.usecase.group.JoinGroupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class GroupNickNameUiState(
    val groupName: String = "",
    val nickName: String = "",
    val nicknameError: NameValidResult.Error? = null,
    val submitError: GroupNickNameError? = null,
    val isConfirmPopupVisible: Boolean = false,
    val isEntering: Boolean = false,
) : UiState

sealed interface GroupNickNameIntent : UiIntent {
    data object ClickNextButton : GroupNickNameIntent

    data object ClickBackButton : GroupNickNameIntent

    data class InputWord(val nickName: String) : GroupNickNameIntent

    data object ClickConfirmPopupEnter : GroupNickNameIntent

    data object DismissConfirmPopup : GroupNickNameIntent
}

sealed interface GroupNickNameSideEffect : UiSideEffect {
    data object NavigateToBack : GroupNickNameSideEffect

    data object NavigateToNext : GroupNickNameSideEffect
}

@HiltViewModel(assistedFactory = GroupNickNameViewModel.Factory::class)
class GroupNickNameViewModel
@AssistedInject
constructor(
    @Assisted(ASSISTED_INVITE_CODE) inviteCodeValue: String,
    @Assisted(ASSISTED_GROUP_NAME) groupName: String,
    private val checkNickNameValid: CheckNameValidUseCase,
    private val joinGroup: JoinGroupUseCase,
    private val changeGroupNickname: ChangeGroupNicknameUseCase,
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(groupName = groupName),
) {
    private val inviteCode = InviteCode(inviteCodeValue)

    override fun processIntent(intent: GroupNickNameIntent) {
        when (intent) {
            GroupNickNameIntent.ClickBackButton -> postSideEffect(GroupNickNameSideEffect.NavigateToBack)

            GroupNickNameIntent.ClickNextButton -> confirmNickname()

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(
                        nickName = intent.nickName,
                        nicknameError = null,
                        submitError = null,
                    )
                }
            }

            GroupNickNameIntent.ClickConfirmPopupEnter -> enterGroup()

            GroupNickNameIntent.DismissConfirmPopup -> {
                if (state.value.isEntering) return

                updateState { copy(isConfirmPopupVisible = false) }
            }
        }
    }

    private fun confirmNickname() {
        when (val result = checkNickNameValid(state.value.nickName)) {
            is NameValidResult.Error -> updateState { copy(nicknameError = result) }
            NameValidResult.Success -> updateState { copy(isConfirmPopupVisible = true) }
        }
    }

    private fun enterGroup() {
        launch(key = KEY_ENTER_GROUP) {
            updateState { copy(submitError = null, isEntering = true) }
            try {
                val joined = joinGroup(inviteCode).getOrElse { throwable ->
                    handleJoinFailure(throwable)
                    return@launch
                }

                changeGroupNickname(groupId = joined.groupId, groupNickname = GroupNickname(state.value.nickName))
                    .onFailure { throwable ->
                        // TODO(닉네임 적용 실패 안내): 참여는 됐고 전역 닉네임이 그대로 쓰이는 상태다.
                        //  "닉네임은 나중에 바꿀 수 있어요" 정도의 토스트를 띄울 자리가 필요하다
                        viewModelLogger.e(throwable) { "그룹 닉네임 적용 실패 — 전역 닉네임을 그대로 쓴다" }
                    }

                updateState { copy(isConfirmPopupVisible = false) }
                postSideEffect(GroupNickNameSideEffect.NavigateToNext)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
                updateState { copy(isEntering = false) }
            }
        }
    }

    /** 실패 갈래를 전부 열거해 둔다. 화면에는 입력 자리 아래 한 줄로만 나간다 */
    private fun handleJoinFailure(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> GroupNickNameError.NETWORK

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.ParfaitGroup.INVALID_INVITE_CODE -> GroupNickNameError.INVALID_INVITE_CODE
                ServerErrorCode.ParfaitGroup.GROUP_ALREADY_JOINED -> GroupNickNameError.ALREADY_JOINED
                ServerErrorCode.ParfaitGroup.GROUP_MEMBER_LIMIT_REACHED -> GroupNickNameError.MEMBER_LIMIT_REACHED
                else -> GroupNickNameError.UNKNOWN
            }

            else -> GroupNickNameError.UNKNOWN
        }

        viewModelLogger.e(throwable) { "그룹 참여 실패 — $error" }
        updateState { copy(isConfirmPopupVisible = false, submitError = error) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted(ASSISTED_INVITE_CODE) inviteCodeValue: String,
            @Assisted(ASSISTED_GROUP_NAME) groupName: String,
        ): GroupNickNameViewModel
    }

    private companion object {
        const val ASSISTED_INVITE_CODE = "inviteCode"

        const val ASSISTED_GROUP_NAME = "groupName"

        /** [launch] 중복 실행 가드 키 — 참여·닉네임 적용을 묶은 job 하나를 가리킨다 */
        const val KEY_ENTER_GROUP = "enterGroup"
    }
}
