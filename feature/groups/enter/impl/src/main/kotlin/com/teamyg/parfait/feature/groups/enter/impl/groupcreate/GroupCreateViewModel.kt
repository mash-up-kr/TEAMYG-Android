package com.teamyg.parfait.feature.groups.enter.impl.groupcreate

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
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

    /**
     * 그룹 생성이 실패했다. 확인 팝업은 이미 닫힌 뒤이므로(팝업은 `Dialog` 라 그 위로는
     * 토스트가 보이지 않는다) 화면이 안내를 띄우고 사용자는 다시 확인을 누르면 된다.
     */
    data object ShowCreateFailure : GroupCreateSideEffect
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
        // `Result.failure` 는 아래 `onFailure` 가, 매퍼 버그 같은 *예상 못 한* 예외는
        // `onError` 가 받는다 — 두 경로 모두 같은 안내로 모아야 실패가 조용히 사라지지 않는다
        launch(key = KEY_CREATE_GROUP, onError = ::onCreateGroupFailed) {
            try {
                createGroup(
                    groupName = GroupName(state.value.groupName),
                    groupNickname = GroupNickname(state.value.nickName),
                    memberLimit = memberLimit,
                ).onSuccess(::onGroupCreated)
                    .onFailure(::onCreateGroupFailed)
            } finally {
                // `finally` 는 예외·취소 어느 경로로 빠져나가도 돈다 — 팝업 버튼이
                // 영구 비활성으로 남는 것을 여기서 막는다
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
     * 실패 갈래를 전부 열거해 둔다. 지금은 어느 갈래든 같은 안내를 띄우지만, 문구가
     * 정해지면 각 자리를 바꾸면 되고 분기를 다시 발굴할 필요가 없다.
     */
    private fun onCreateGroupFailed(throwable: Throwable) {
        when (throwable) {
            is AppError.Network ->
                // TODO(에러 UX 미정): "네트워크 연결을 확인해 주세요" + 재시도 안내
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 네트워크 단절" }

            is AppError.Server ->
                // TODO(에러 UX 미정): 서버가 준 사유별 안내
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 서버 에러 ${throwable.code}" }

            else ->
                // groupId 가 유효하지 않은 응답·매핑 실패가 여기로 온다
                viewModelLogger.e(throwable) { "그룹 생성 실패 — 예상하지 못한 오류" }
        }

        updateState { copy(isConfirmPopupVisible = false) }
        postSideEffect(GroupCreateSideEffect.ShowCreateFailure)
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
