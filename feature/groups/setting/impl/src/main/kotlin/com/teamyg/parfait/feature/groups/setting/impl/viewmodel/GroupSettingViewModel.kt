package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.error.ServerErrorCode
import com.teamyg.parfait.domain.model.group.GroupDetailVO
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import com.teamyg.parfait.feature.groups.setting.impl.model.GroupMemberUiModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 그룹 설정 화면이 한 번에 하나만 띄울 수 있는 확인 팝업. */
enum class GroupSettingDialog {
    Leave,
    Report,
}

data class GroupSettingUiState(
    val groupName: GroupName = GroupName(""),
    val myNickname: GroupNickname = GroupNickname(""),
    val nicknameInput: String = "",
    val isEditing: Boolean = false,
    val nicknameError: NameValidResult.Error? = null,
    /** 서버가 닉네임 변경을 되돌린 사유. 입력 형식 오류인 [nicknameError] 와 자리가 다르다 */
    val submitError: GroupSettingError? = null,
    val members: List<GroupMemberUiModel> = emptyList(),
    val inviteCode: InviteCode = InviteCode(""),
    // TODO: 정원이 그룹 생성 응답에만 있어 남은 자리를 셀 수 없다. 서버가 상세에 memberLimit 을
    //  실어 주면 `memberLimit - members.size` 로 바꾼다
    val remainingCount: Int = MOCK_REMAINING_COUNT,
    val isCodeCopied: Boolean = false,
    val visibleDialog: GroupSettingDialog? = null,
    /** 닉네임 변경이 서버에 오가는 중. 확인 버튼을 다시 누르지 못하게 막는다 */
    val isSubmittingNickname: Boolean = false,
) : UiState {
    /** 입력값이 유효한가. 키보드 Done 처리처럼 "닫아도 되는가"의 기준. */
    val isNicknameValid: Boolean
        get() = nicknameError == null

    /** 실제로 서버에 보낼 변경이 있는가. 확인 버튼 활성 기준. */
    val isConfirmEnabled: Boolean
        get() = isNicknameValid && nicknameInput != myNickname.value && !isSubmittingNickname
}

sealed interface GroupSettingIntent : UiIntent {
    data object ClickBack : GroupSettingIntent

    data class InputNickname(val nickname: String) : GroupSettingIntent

    data class ChangeNicknameFocus(val isFocused: Boolean) : GroupSettingIntent

    data object ConfirmNickname : GroupSettingIntent

    data object ClickCopyInviteCode : GroupSettingIntent

    data object ClickLeaveGroup : GroupSettingIntent

    data object ClickReportGroup : GroupSettingIntent

    data object ConfirmLeaveGroup : GroupSettingIntent

    data object ConfirmReportGroup : GroupSettingIntent

    data object DismissDialog : GroupSettingIntent
}

sealed interface GroupSettingSideEffect : UiSideEffect {
    data object NavigateBack : GroupSettingSideEffect

    data class CopyInviteCode(val inviteCode: String) : GroupSettingSideEffect
}

@HiltViewModel(assistedFactory = GroupSettingViewModel.Factory::class)
class GroupSettingViewModel
@AssistedInject
constructor(
    @Assisted groupIdValue: Long,
    private val checkNameValid: CheckNameValidUseCase,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
    private val changeGroupNickname: ChangeGroupNicknameUseCase,
) : BaseViewModel<GroupSettingUiState, GroupSettingIntent, GroupSettingSideEffect>(
    initialState = GroupSettingUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    private var copyResetJob: Job? = null

    init {
        viewModelLogger.i { "GroupSettingViewModel::init" }
        loadGroupDetail()
    }

    /**
     * 실패해도 알릴 자리가 없어 로그만 남긴다 — 화면은 빈 값 그대로 뜬다.
     * TODO(에러 UX 미정): 이 화면이 실패를 표현할 방법이 정해지면 여기서 상태로 올린다
     */
    private fun loadGroupDetail() {
        launch(key = KEY_LOAD_GROUP_DETAIL) {
            val myMemberId = getMyAccountFlow().first()?.memberId

            getGroupDetail(groupId)
                .onSuccess { detail -> updateState { withDetail(detail, myMemberId) } }
                .onFailure { throwable ->
                    viewModelLogger.e(throwable) { "그룹 상세를 불러오지 못했다 - groupId: ${groupId.value}" }
                }
        }
    }

    /**
     * 편집 중이면 입력값은 건드리지 않는다 — 조회가 늦게 끝났다고 사용자가 치던 글자를
     * 지워 버릴 이유가 없다.
     */
    private fun GroupSettingUiState.withDetail(
        detail: GroupDetailVO,
        myMemberId: MemberId?,
    ): GroupSettingUiState = copy(
        groupName = detail.groupName,
        myNickname = detail.myNickname,
        nicknameInput = if (isEditing) nicknameInput else detail.myNickname.value,
        inviteCode = detail.inviteCode,
        members = detail.members.toUiModels(myMemberId),
    )

    /**
     * 서버가 칩 색을 주지 않아 목록 순서로 팔레트를 돌려 쓴다.
     *
     * [myMemberId] 를 모르면 아무도 나로 표시되지 않는다 — 그룹 닉네임은 중복될 수 있어
     * 이름으로 나를 찾으면 남을 나로 표시할 수 있다.
     */
    private fun List<ParfaitGroupMemberVO>.toUiModels(myMemberId: MemberId?): List<GroupMemberUiModel> =
        mapIndexed { index, member ->
            GroupMemberUiModel(
                id = member.memberId.value,
                nickname = member.groupNickname.value,
                colorChipType = NAMETAG_CHIP_TYPES[index % NAMETAG_CHIP_TYPES.size],
                isMe = member.memberId == myMemberId,
            )
        }

    override fun processIntent(intent: GroupSettingIntent) {
        when (intent) {
            GroupSettingIntent.ClickBack -> handleClickBack()
            is GroupSettingIntent.InputNickname -> handleInputNickname(intent.nickname)
            is GroupSettingIntent.ChangeNicknameFocus -> handleChangeNicknameFocus(intent.isFocused)
            GroupSettingIntent.ConfirmNickname -> handleConfirmNickname()
            GroupSettingIntent.ClickCopyInviteCode -> handleClickCopyInviteCode()
            GroupSettingIntent.ClickLeaveGroup -> handleClickLeaveGroup()
            GroupSettingIntent.ClickReportGroup -> handleClickReportGroup()
            GroupSettingIntent.ConfirmLeaveGroup -> handleConfirmLeaveGroup()
            GroupSettingIntent.ConfirmReportGroup -> handleConfirmReportGroup()
            GroupSettingIntent.DismissDialog -> handleDismissDialog()
        }
    }

    private fun handleClickBack() {
        if (state.value.isEditing) {
            cancelEditing()
        } else {
            postSideEffect(GroupSettingSideEffect.NavigateBack)
        }
    }

    private fun handleInputNickname(nickname: String) {
        val nicknameError = checkNameValid(nickname) as? NameValidResult.Error

        updateState {
            copy(
                nicknameInput = nickname,
                nicknameError = nicknameError,
                submitError = null,
            )
        }
    }

    private fun handleChangeNicknameFocus(isFocused: Boolean) {
        if (isFocused) {
            updateState { copy(isEditing = true) }
        } else {
            cancelEditing()
        }
    }

    /**
     * 서버가 받아 준 이름만 화면에 남긴다 — 먼저 반영해 두고 실패하면 되돌리는 방식은,
     * 되돌아간 이름을 사용자가 "저장됐다"고 읽은 뒤라 더 헷갈린다.
     */
    private fun handleConfirmNickname() {
        if (!state.value.isConfirmEnabled) return

        val nickname = GroupNickname(state.value.nicknameInput)

        launch(key = KEY_CHANGE_NICKNAME) {
            updateState { copy(isSubmittingNickname = true, submitError = null) }
            try {
                changeGroupNickname(groupId = groupId, groupNickname = nickname)
                    .onSuccess { changed -> updateState { withMyNickname(changed.groupNickname) } }
                    .onFailure { throwable -> updateState { copy(submitError = throwable.toSubmitError()) } }
            } finally {
                // 예외·취소 어느 경로로 빠져나가도 버튼이 영구 비활성으로 남지 않게 한다
                updateState { copy(isSubmittingNickname = false) }
            }
        }
    }

    private fun GroupSettingUiState.withMyNickname(nickname: GroupNickname): GroupSettingUiState = copy(
        myNickname = nickname,
        nicknameInput = nickname.value,
        members = members.map { member ->
            if (member.isMe) member.copy(nickname = nickname.value) else member
        },
        isEditing = false,
        nicknameError = null,
    )

    /** 실패 갈래를 전부 열거해 둔다. 화면에는 입력 자리 아래 한 줄로만 나간다 */
    private fun Throwable.toSubmitError(): GroupSettingError {
        viewModelLogger.e(this) { "그룹 닉네임을 바꾸지 못했다 - groupId: ${groupId.value}" }

        return when (this) {
            is AppError.Network -> GroupSettingError.NETWORK

            is AppError.Server -> when (code) {
                ServerErrorCode.ParfaitGroup.INVALID_GROUP_NICKNAME -> GroupSettingError.INVALID_NICKNAME
                else -> GroupSettingError.UNKNOWN
            }

            else -> GroupSettingError.UNKNOWN
        }
    }

    private fun handleClickCopyInviteCode() {
        updateState { copy(isCodeCopied = true) }
        postSideEffect(GroupSettingSideEffect.CopyInviteCode(state.value.inviteCode.value))

        copyResetJob?.cancel()
        copyResetJob = viewModelScope.launch {
            delay(COPY_CODE_RESET_DELAY_MS)
            updateState { copy(isCodeCopied = false) }
        }
    }

    private fun handleClickLeaveGroup() {
        updateState { copy(visibleDialog = GroupSettingDialog.Leave) }
    }

    private fun handleClickReportGroup() {
        updateState { copy(visibleDialog = GroupSettingDialog.Report) }
    }

    private fun handleConfirmLeaveGroup() {
        if (state.value.visibleDialog != GroupSettingDialog.Leave) return

        updateState { copy(visibleDialog = null) }
        // TODO: 그룹 나가기 API 연동 (DELETE /api/parfait-groups/{groupId}/members/me)
        viewModelLogger.i { "GroupSettingViewModel::handleConfirmLeaveGroup" }
    }

    private fun handleConfirmReportGroup() {
        if (state.value.visibleDialog != GroupSettingDialog.Report) return

        updateState { copy(visibleDialog = null) }
        // TODO: 그룹 신고 API 연동 (POST /api/parfait-groups/{groupId}/reports)
        viewModelLogger.i { "GroupSettingViewModel::handleConfirmReportGroup" }
    }

    private fun handleDismissDialog() {
        updateState { copy(visibleDialog = null) }
    }

    @AssistedFactory
    interface Factory {
        fun create(groupIdValue: Long): GroupSettingViewModel
    }

    private companion object {
        const val KEY_LOAD_GROUP_DETAIL = "loadGroupDetail"
        const val KEY_CHANGE_NICKNAME = "changeNickname"
    }

    private fun cancelEditing() {
        updateState {
            copy(
                nicknameInput = myNickname.value,
                nicknameError = null,
                submitError = null,
                isEditing = false,
            )
        }
    }
}

private const val MOCK_REMAINING_COUNT = 1

// 초대 코드 복사 후 "복사됨" 문구가 원래 문구로 되돌아가기까지의 지연(ms)
private const val COPY_CODE_RESET_DELAY_MS = 2000L

// TODO: 컬러칩 타입 부여 주체가 미정이라 목록 인덱스로 순환 배정한다. 서버가 타입을 주면 교체.
private val NAMETAG_CHIP_TYPES: List<YGColorChipType> = listOf(
    YGColorChipType.NametagChip1,
    YGColorChipType.NametagChip2,
    YGColorChipType.NametagChip3,
    YGColorChipType.NametagChip4,
    YGColorChipType.NametagChip5,
    YGColorChipType.NametagChip6,
    YGColorChipType.NametagChip7,
    YGColorChipType.NametagChip8,
    YGColorChipType.NametagChip9,
    YGColorChipType.NametagChip10,
    YGColorChipType.NametagChip11,
    YGColorChipType.NametagChip12,
)
