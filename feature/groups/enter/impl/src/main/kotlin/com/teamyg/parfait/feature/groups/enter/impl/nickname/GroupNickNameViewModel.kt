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
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

data class GroupNickNameUiState(
    val nickName: String = "",
    val nicknameError: NameValidResult.Error? = null,
    val submitError: GroupNickNameError? = null,
    val isEntering: Boolean = false,
) : UiState

sealed interface GroupNickNameIntent : UiIntent {
    data object ClickNextButton : GroupNickNameIntent

    data object ClickBackButton : GroupNickNameIntent

    data class InputWord(val nickName: String) : GroupNickNameIntent
}

sealed interface GroupNickNameSideEffect : UiSideEffect {
    data object NavigateToBack : GroupNickNameSideEffect

    data object NavigateToNext : GroupNickNameSideEffect
}

@HiltViewModel(assistedFactory = GroupNickNameViewModel.Factory::class)
class GroupNickNameViewModel
@AssistedInject
constructor(
    @Assisted groupIdValue: Long,
    private val checkNickNameValid: CheckNameValidUseCase,
    private val changeGroupNickname: ChangeGroupNicknameUseCase,
) : BaseViewModel<GroupNickNameUiState, GroupNickNameIntent, GroupNickNameSideEffect>(
    initialState = GroupNickNameUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    override fun processIntent(intent: GroupNickNameIntent) {
        when (intent) {
            GroupNickNameIntent.ClickBackButton -> postSideEffect(GroupNickNameSideEffect.NavigateToBack)

            is GroupNickNameIntent.ClickNextButton -> requestChangeNickname()

            is GroupNickNameIntent.InputWord -> {
                updateState {
                    copy(
                        nickName = intent.nickName,
                        nicknameError = null,
                        submitError = null,
                    )
                }
            }
        }
    }

    private fun requestChangeNickname() {
        val nickName = state.value.nickName
        when (val result = checkNickNameValid(nickName)) {
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
                    isEntering = true,
                )
            }
            try {
                changeGroupNickname(groupId = groupId, groupNickname = GroupNickname(nickName))
                    .onSuccess { postSideEffect(GroupNickNameSideEffect.NavigateToNext) }
                    .onFailure(::handleFailure)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
                updateState { copy(isEntering = false) }
            }
        }
    }

    /** 실패 갈래를 전부 열거해 둔다. 화면에는 입력 자리 아래 한 줄로만 나간다 */
    private fun handleFailure(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> GroupNickNameError.NETWORK

            is AppError.Server -> when (throwable.code) {
                ServerErrorCode.ParfaitGroup.INVALID_GROUP_NICKNAME -> GroupNickNameError.INVALID
                else -> GroupNickNameError.UNKNOWN
            }

            else -> GroupNickNameError.UNKNOWN
        }

        viewModelLogger.e(throwable) { "그룹 닉네임 적용 실패 — $error" }
        updateState { copy(submitError = error) }
    }

    @AssistedFactory
    interface Factory {
        fun create(groupIdValue: Long): GroupNickNameViewModel
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 닉네임 적용 job 하나를 가리킨다 */
        const val KEY_CHANGE_NICKNAME = "changeGroupNickname"
    }
}
