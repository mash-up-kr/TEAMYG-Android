package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.CreatedGroupVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.CreateGroupUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel

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

            GroupCreateIntent.ClickConfirmPopupCreate -> requestCreateGroup()
        }
    }

    private fun requestCreateGroup() {
        val memberLimit = state.value.groupNumber ?: return
        if (state.value.isCreating) return

        updateState { copy(isCreating = true) }
        // `onError` 는 `Result.failure` 가 아니라 던져진 예외를 받는다 — 두 경로를 한곳으로
        // 모아야 실패가 조용히 사라지지 않는다
        launch(key = KEY_CREATE_GROUP, onError = ::onCreateGroupFailed) {
            try {
                createGroup(
                    groupName = GroupName(state.value.groupName),
                    groupNickname = GroupNickname(state.value.nickName),
                    memberLimit = memberLimit,
                ).onSuccess(::onGroupCreated)
                    .onFailure(::onCreateGroupFailed)
            } finally {
                // 어느 경로로 빠져나가도 풀어야 팝업 버튼이 영구 비활성으로 남지 않는다
                updateState { copy(isCreating = false) }
            }
        }
    }

    private fun onGroupCreated(createdGroup: CreatedGroupVO) {
        viewModelLogger.i { "그룹 생성 성공 — groupId=${createdGroup.groupId.value}" }
        updateState { copy(isConfirmPopupVisible = false) }
        postSideEffect(GroupCreateSideEffect.NavigateToNext)
    }

    /**
     * TODO : 실패를 사용자에게 어떻게 알릴지 논의가 끝나지 않아 지금은 로그만 남긴다 — 추후 처리 예정.
     *
     * 팝업은 닫지 않는다. 안내가 없는 지금 닫으면 아무 일도 없던 것처럼 보인다.
     */
    private fun onCreateGroupFailed(throwable: Throwable) {
        when (throwable) {
            is AppError.Network ->
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 네트워크 단절" }

            is AppError.Server -> logServerFailure(throwable)

            else ->
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 예상하지 못한 오류" }
        }
    }

    /** 400 세 갈래는 입력 검증·인원수 UI 가 이미 막으므로, 여기까지 왔다면 서버 규칙과 어긋난 것이다 */
    private fun logServerFailure(error: AppError.Server) {
        when (error.code) {
            ServerErrorCode.ParfaitGroup.INVALID_GROUP_NAME ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 그룹명 규칙 위반(클라 검증과 서버 규칙이 어긋났다)" }

            ServerErrorCode.ParfaitGroup.INVALID_GROUP_NICKNAME ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 닉네임 규칙 위반(클라 검증과 서버 규칙이 어긋났다)" }

            ServerErrorCode.ParfaitGroup.INVALID_GROUP_MEMBER_LIMIT ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 정원이 1~12 밖이다(선택 UI 와 서버 규칙이 어긋났다)" }

            // TODO : 재시도해도 계속 실패하므로 로그인 화면으로 보내야 한다 — 동선 확정 후 처리 예정
            ServerErrorCode.ParfaitGroup.MEMBER_NOT_FOUND ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 토큰의 회원이 서버에 없다(재로그인 필요)" }

            ServerErrorCode.Common.INVALID_REQUEST ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 요청 본문이 서버 계약과 맞지 않다(앱 버그)" }

            else ->
                viewModelLogger.e(error) { "그룹 생성 실패 — 미분류 서버 에러 ${error.code}" }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(nickName: String): GroupCreateViewModel
    }

    private companion object {
        /** [launch] 중복 실행 가드 키 — 이 ViewModel 의 그룹 생성 job 하나를 가리킨다 */
        const val KEY_CREATE_GROUP = "createGroup"
    }
}
