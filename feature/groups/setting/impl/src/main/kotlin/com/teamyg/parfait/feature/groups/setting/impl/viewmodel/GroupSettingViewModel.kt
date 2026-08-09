package com.teamyg.parfait.feature.groups.setting.impl.viewmodel

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.domain.model.NameValidResult
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.feature.groups.setting.impl.model.GroupMemberUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GroupSettingUiState(
    val groupName: GroupName = GroupName(MOCK_GROUP_NAME),
    val myNickname: GroupNickname = GroupNickname(MOCK_MY_NICKNAME),
    val nicknameInput: String = MOCK_MY_NICKNAME,
    val isEditing: Boolean = false,
    val nicknameError: NameValidResult.Error? = null,
    val members: List<GroupMemberUiModel> = MOCK_MEMBERS,
    val inviteCode: InviteCode = InviteCode(MOCK_INVITE_CODE),
    val remainingCount: Int = MOCK_REMAINING_COUNT,
    val isCodeCopied: Boolean = false,
) : UiState {
    val isConfirmEnabled: Boolean
        get() = nicknameError == null && nicknameInput != myNickname.value
}

sealed interface GroupSettingIntent : UiIntent {
    data object ClickBack : GroupSettingIntent

    data class InputNickname(val nickname: String) : GroupSettingIntent

    data class ChangeNicknameFocus(val isFocused: Boolean) : GroupSettingIntent

    data object ConfirmNickname : GroupSettingIntent

    data object ClickCopyInviteCode : GroupSettingIntent

    data object ClickLeaveGroup : GroupSettingIntent

    data object ClickReportGroup : GroupSettingIntent
}

sealed interface GroupSettingSideEffect : UiSideEffect {
    data object NavigateBack : GroupSettingSideEffect

    data class CopyInviteCode(val inviteCode: String) : GroupSettingSideEffect
}

@HiltViewModel
class GroupSettingViewModel
@Inject
constructor(
    private val checkNameValid: CheckNameValidUseCase,
) : BaseViewModel<GroupSettingUiState, GroupSettingIntent, GroupSettingSideEffect>(
    initialState = GroupSettingUiState(),
) {
    private var copyResetJob: Job? = null

    init {
        viewModelLogger.i { "GroupSettingViewModel::init" }
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

    private fun handleConfirmNickname() {
        if (!state.value.isConfirmEnabled) return

        // TODO: 닉네임 변경 API 연동 (PATCH /api/parfait-groups/{groupId}/nickname)
        updateState {
            copy(
                myNickname = GroupNickname(nicknameInput),
                members = members.map { member ->
                    if (member.isMe) member.copy(nickname = nicknameInput) else member
                },
                isEditing = false,
                nicknameError = null,
            )
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
        // TODO: 그룹 나가기 확인 모달 + DELETE /api/parfait-groups/{groupId}/members/me
        viewModelLogger.i { "GroupSettingViewModel::handleClickLeaveGroup" }
    }

    private fun handleClickReportGroup() {
        // TODO: 그룹 신고 확인 모달 + POST /api/parfait-groups/{groupId}/reports
        viewModelLogger.i { "GroupSettingViewModel::handleClickReportGroup" }
    }

    private fun cancelEditing() {
        updateState {
            copy(
                nicknameInput = myNickname.value,
                nicknameError = null,
                isEditing = false,
            )
        }
    }
}

// TODO: 그룹 상세 조회 API 연동 시 아래 Mock 전량 교체
//  (GET /api/parfait-groups/{groupId} — groupName·memberLimit는 계약에 없어 별도 확보 필요)
private const val MOCK_GROUP_NAME = "그룹이름"
private const val MOCK_MY_NICKNAME = "잠탈전용닉네임2"
private const val MOCK_INVITE_CODE = "WDIDCJ"
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

private val MOCK_MEMBER_NICKNAMES = listOf(
    MOCK_MY_NICKNAME,
    "아니야나그런데기니야기니라니까",
    "체리마루",
    "멜론소다먹고싶다",
    "푸딩왕자",
    "딸기시럽듬뿍",
    "오레오조각",
    "노랑젤리",
    "파랑젤리",
    "키위한조각",
    "생크림가득",
)

private val MOCK_MEMBERS: List<GroupMemberUiModel> = MOCK_MEMBER_NICKNAMES.mapIndexed { index, nickname ->
    GroupMemberUiModel(
        nickname = nickname,
        colorChipType = NAMETAG_CHIP_TYPES[index % NAMETAG_CHIP_TYPES.size],
        isMe = index == 0,
    )
}
