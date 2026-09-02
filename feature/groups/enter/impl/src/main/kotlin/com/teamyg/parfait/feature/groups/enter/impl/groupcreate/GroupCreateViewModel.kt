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
    val nickNameError: NameValidResult.Error? = null,
    val isConfirmPopupVisible: Boolean = false,
    val isCreating: Boolean = false,
) : UiState {
    val isValid = groupName.isNotEmpty() && nickName.isNotEmpty() && groupNumber != null
}

sealed interface GroupCreateIntent : UiIntent {
    data object ClickNextButton : GroupCreateIntent

    data object ClickBackButton : GroupCreateIntent

    data class InputGroupName(val newGroupName: String) : GroupCreateIntent

    data class InputNickName(val newNickName: String) : GroupCreateIntent

    data class ClickGroupNumber(val newSelectedNumber: Int) : GroupCreateIntent

    data object ClickConfirmPopupCreate : GroupCreateIntent

    data object DismissConfirmPopup : GroupCreateIntent
}

sealed interface GroupCreateSideEffect : UiSideEffect {
    data object NavigateToBack : GroupCreateSideEffect

    data class NavigateToNext(
        val groupId: Long,
        val groupName: String,
        val inviteCode: String,
    ) : GroupCreateSideEffect

    data class ShowError(val error: GroupCreateError) : GroupCreateSideEffect
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

            is GroupCreateIntent.InputNickName -> {
                updateState {
                    copy(
                        nickName = intent.newNickName,
                        nickNameError = null,
                    )
                }
            }

            GroupCreateIntent.ClickNextButton -> confirmForm()

            GroupCreateIntent.DismissConfirmPopup -> {
                updateState { copy(isConfirmPopupVisible = false) }
            }

            GroupCreateIntent.ClickConfirmPopupCreate -> requestCreateGroup()
        }
    }

    /** 두 이름을 함께 검사한다 — 한쪽만 보면 통과한 뒤 다른 쪽 에러가 뒤늦게 떠 두 번 걸린다 */
    private fun confirmForm() {
        val groupNameError = checkNameValid(state.value.groupName) as? NameValidResult.Error
        val nickNameError = checkNameValid(state.value.nickName) as? NameValidResult.Error

        updateState {
            copy(
                groupNameError = groupNameError,
                nickNameError = nickNameError,
                isConfirmPopupVisible = groupNameError == null && nickNameError == null,
            )
        }
    }

    private fun requestCreateGroup() {
        val memberLimit = state.value.groupNumber ?: return
        if (state.value.isCreating) return

        // 통신을 시작하기 전에 팝업을 걷는다 — 진행 중임은 `isCreating` 하나로만 말한다
        updateState { copy(isConfirmPopupVisible = false, isCreating = true) }
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
        postSideEffect(
            GroupCreateSideEffect.NavigateToNext(
                groupId = createdGroup.groupId.value,
                groupName = createdGroup.groupName.value,
                inviteCode = createdGroup.inviteCode.value,
            ),
        )
    }

    /** 입력값은 지우지 않는다 — 실패는 대개 재시도로 풀린다 */
    private fun onCreateGroupFailed(throwable: Throwable) {
        val error = when (throwable) {
            is AppError.Network -> {
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 네트워크 단절" }
                GroupCreateError.NETWORK
            }

            is AppError.Server -> {
                logServerFailure(throwable)
                GroupCreateError.UNKNOWN
            }

            else -> {
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 예상하지 못한 오류" }
                GroupCreateError.UNKNOWN
            }
        }

        postSideEffect(GroupCreateSideEffect.ShowError(error))
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
