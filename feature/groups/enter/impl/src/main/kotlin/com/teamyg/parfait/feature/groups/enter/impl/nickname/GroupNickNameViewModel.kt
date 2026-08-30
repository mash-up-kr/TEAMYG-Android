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
import kotlinx.coroutines.delay

data class GroupNickNameUiState(
    val groupName: String = "",
    val nickName: String = "",
    val nicknameError: NameValidResult.Error? = null,
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

    data class NavigateToNext(val groupId: Long, val groupName: String) : GroupNickNameSideEffect

    data class ShowError(val error: GroupNickNameError) : GroupNickNameSideEffect
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
                    )
                }
            }

            GroupNickNameIntent.ClickConfirmPopupEnter -> enterGroup()

            GroupNickNameIntent.DismissConfirmPopup -> {
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
        // 팝업을 먼저 걷는다 — 진행 중임은 `isEntering` 하나로만 말한다
        updateState { copy(isConfirmPopupVisible = false, isEntering = true) }

        launch(key = KEY_ENTER_GROUP, onError = ::onEnterGroupThrown) {
            try {
                val joined = joinGroup(inviteCode).getOrElse { throwable ->
                    handleJoinFailure(throwable)
                    return@launch
                }

                changeGroupNickname(groupId = joined.groupId, groupNickname = GroupNickname(state.value.nickName))
                    .onFailure { throwable ->
                        viewModelLogger.e(throwable) { "그룹 닉네임 적용 실패 — 전역 닉네임을 그대로 쓴다" }
                        noticeNicknameNotApplied()
                    }

                postSideEffect(
                    GroupNickNameSideEffect.NavigateToNext(
                        groupId = joined.groupId.value,
                        groupName = joined.groupName.value,
                    ),
                )
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 화면이
                // 로딩 오버레이에 갇히는 것을 여기서 막는다
                updateState { copy(isEntering = false) }
            }
        }
    }

    /**
     * 안내를 띄운 뒤 그것이 사라질 때까지 화면을 붙잡는다 — 토스트는 이 화면에 매여 있어서
     * 곧바로 넘기면 뜨자마자 같이 사라진다.
     *
     * 기다리는 동안 [GroupNickNameUiState.isEntering] 을 켠 채로 둔다. 참여는 이미 끝났으므로
     * 여기서 다시 참여하기를 누르면 이미 참여한 그룹이라는 실패만 돌아온다.
     */
    private suspend fun noticeNicknameNotApplied() {
        postSideEffect(GroupNickNameSideEffect.ShowError(GroupNickNameError.NICKNAME_NOT_APPLIED))
        delay(NICKNAME_NOTICE_DURATION)
    }

    /** `Result.failure` 가 아니라 예외로 튄 경로 — 갈래를 가릴 수 없어 한 갈래로 접는다 */
    private fun onEnterGroupThrown(error: AppError) {
        viewModelLogger.e(error) { "그룹 참여 실패 — 예상하지 못한 오류" }
        postSideEffect(GroupNickNameSideEffect.ShowError(GroupNickNameError.UNKNOWN))
    }

    /** 실패 갈래를 전부 열거해 둔다. 화면에는 토스트 한 줄로만 나간다 */
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
        postSideEffect(GroupNickNameSideEffect.ShowError(error))
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

        /** 토스트가 스스로 사라지는 시간과 맞춘다(`YGToastPolicy`) */
        const val NICKNAME_NOTICE_DURATION = 2_000L
    }
}
