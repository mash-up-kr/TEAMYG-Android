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
import com.teamyg.parfait.domain.model.group.GroupName
import com.teamyg.parfait.domain.model.group.GroupNickname
import com.teamyg.parfait.domain.model.group.InviteCode
import com.teamyg.parfait.domain.model.group.NametagChipType
import com.teamyg.parfait.domain.model.group.ParfaitGroupDetailVO
import com.teamyg.parfait.domain.model.group.ParfaitGroupMemberVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.id.MemberId
import com.teamyg.parfait.domain.usecase.CheckNameValidUseCase
import com.teamyg.parfait.domain.usecase.group.ChangeGroupNicknameUseCase
import com.teamyg.parfait.domain.usecase.group.GetGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.LeaveGroupUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshGroupDetailUseCase
import com.teamyg.parfait.domain.usecase.group.ReportGroupUseCase
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
    /** 입력 형식 오류. 서버가 되돌린 사유는 상태가 아니라 [GroupSettingSideEffect.ShowError] 로 나간다 */
    val nicknameError: NameValidResult.Error? = null,
    val members: List<GroupMemberUiModel> = emptyList(),
    val inviteCode: InviteCode = InviteCode(""),
    val remainingCount: Int = 0,
    val isCodeCopied: Boolean = false,
    val visibleDialog: GroupSettingDialog? = null,
    /** 닉네임 변경이 서버에 오가는 중. 확인 버튼을 다시 누르지 못하게 막는다 */
    val isSubmittingNickname: Boolean = false,
    /** 첫 조회가 끝나기 전. 화면에 아직 아무 값도 없는 구간이다 */
    val isLoadingDetail: Boolean = true,
    /** 나가기·신고가 서버에 오가는 중. 둘은 한 번에 하나만 뜨는 팝업에서 나와 함께 세지 않는다 */
    val isSubmittingDialogAction: Boolean = false,
) : UiState {
    /**
     * 화면을 덮어야 하는 대기. 첫 조회는 보여 줄 값이 없어서, 닉네임 변경은 왕복이 끝나기 전에
     * 입력 필드를 더 고칠 수 있어서, 나가기·신고는 끝나면 이 화면을 떠나서 덮는다.
     */
    val isLoading: Boolean
        get() = isLoadingDetail || isSubmittingNickname || isSubmittingDialogAction

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

    /**
     * 서버가 되돌린 사유. 입력 형식 오류는 고칠 곳이 눈앞에 있어 입력칸 아래 남지만, 서버 실패는
     * 그 자리에서 고칠 수 있는 것이 아니라 공용 토스트로 나간다.
     */
    data class ShowError(val error: GroupSettingError) : GroupSettingSideEffect

    /** 그룹에서 빠져나온 뒤 갈 곳. 뒤로 가면 방금 떠난 그룹의 화면이라 되돌아갈 수 없어야 한다 */
    data object NavigateToGroupList : GroupSettingSideEffect
}

@HiltViewModel(assistedFactory = GroupSettingViewModel.Factory::class)
class GroupSettingViewModel
@AssistedInject
constructor(
    @Assisted groupIdValue: Long,
    private val checkNameValid: CheckNameValidUseCase,
    private val getGroupDetail: GetGroupDetailUseCase,
    private val refreshGroupDetail: RefreshGroupDetailUseCase,
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
    private val changeGroupNickname: ChangeGroupNicknameUseCase,
    private val leaveGroup: LeaveGroupUseCase,
    private val reportGroup: ReportGroupUseCase,
) : BaseViewModel<GroupSettingUiState, GroupSettingIntent, GroupSettingSideEffect>(
    initialState = GroupSettingUiState(),
) {
    private val groupId = GroupId(groupIdValue)

    private var copyResetJob: Job? = null

    init {
        viewModelLogger.i { "GroupSettingViewModel::init" }
        observeGroupDetail()
        loadGroupDetail()
    }

    /**
     * 상세는 캐시가 낸다 — 닉네임을 바꾸면 저장소가 서버에서 다시 받아 캐시에 넣고, 그 값이
     * 여기로 내려온다. 화면이 자기 상태를 손으로 고치지 않는다.
     */
    private fun observeGroupDetail() {
        // 계정 조회(EncryptedPreferences → DataStore IO)가 실패해도 상세는 계속 떠야 한다 —
        // 그래서 `first()` 만 감싸 실패를 `null` 로 접고, 구독 자체는 이어간다. 바깥을
        // `launch`(가드 있는 쪽)로 감싸는 것은 여기서 예상 못 한 예외까지 잡기 위한 방어선이다.
        launch {
            val myMemberId = runCatching { getMyAccountFlow().first() }.getOrNull()?.memberId

            getGroupDetail(groupId).collect { detail ->
                if (detail == null) return@collect
                updateState { withDetail(detail, myMemberId).copy(isLoadingDetail = false) }
            }
        }
    }

    private fun loadGroupDetail() {
        launch(key = KEY_LOAD_GROUP_DETAIL) {
            try {
                refreshGroupDetail(groupId).onFailure { throwable ->
                    viewModelLogger.e(throwable) { "그룹 상세를 불러오지 못했다 - groupId: ${groupId.value}" }
                    postSideEffect(GroupSettingSideEffect.ShowError(throwable.toGroupSettingError()))
                }
            } finally {
                // 예외·취소 어느 경로로 빠져나가도 로딩이 걸린 채 남지 않게 한다
                updateState { copy(isLoadingDetail = false) }
            }
        }
    }

    /**
     * 편집 중이면 입력값은 건드리지 않는다 — 조회가 늦게 끝났다고 사용자가 치던 글자를
     * 지워 버릴 이유가 없다.
     */
    private fun GroupSettingUiState.withDetail(
        detail: ParfaitGroupDetailVO,
        myMemberId: MemberId?,
    ): GroupSettingUiState = copy(
        groupName = detail.groupName,
        myNickname = detail.groupNickname,
        nicknameInput = if (isEditing) nicknameInput else detail.groupNickname.value,
        inviteCode = detail.inviteCode,
        // 캐시와 서버가 어긋나 멤버가 정원을 넘으면 음수가 난다 — 0 아래로는 뜻이 없다
        remainingCount = (detail.memberLimit - detail.members.size).coerceAtLeast(0),
        members = detail.members.toUiModels(myMemberId),
    )

    /**
     * [myMemberId] 를 모르면 아무도 나로 표시되지 않는다 — 그룹 닉네임은 중복될 수 있어
     * 이름으로 나를 찾으면 남을 나로 표시할 수 있다.
     */
    private fun List<ParfaitGroupMemberVO>.toUiModels(myMemberId: MemberId?): List<GroupMemberUiModel> = map { member ->
        GroupMemberUiModel(
            id = member.memberId.value,
            nickname = member.groupNickname.value,
            colorChipType = member.nametagChip.toColorChipType(),
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
            updateState { copy(isSubmittingNickname = true) }
            try {
                changeGroupNickname(groupId = groupId, groupNickname = nickname)
                    .onSuccess { updateState { copy(isEditing = false, nicknameError = null) } }
                    .onFailure { throwable ->
                        viewModelLogger.e(throwable) { "그룹 닉네임을 바꾸지 못했다 - groupId: ${groupId.value}" }
                        postSideEffect(GroupSettingSideEffect.ShowError(throwable.toGroupSettingError()))
                    }
            } finally {
                // 예외·취소 어느 경로로 빠져나가도 버튼이 영구 비활성으로 남지 않게 한다
                updateState { copy(isSubmittingNickname = false) }
            }
        }
    }

    /** 실패 갈래를 전부 열거해 둔다 */
    private fun Throwable.toGroupSettingError(): GroupSettingError = when (this) {
        is AppError.Network -> GroupSettingError.NETWORK

        is AppError.Server -> when (code) {
            ServerErrorCode.ParfaitGroup.INVALID_GROUP_NICKNAME -> GroupSettingError.INVALID_NICKNAME
            else -> GroupSettingError.UNKNOWN
        }

        else -> GroupSettingError.UNKNOWN
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

        submitDialogAction(key = KEY_LEAVE_GROUP, action = LEAVE_ACTION) {
            leaveGroup(groupId)
        }
    }

    /**
     * 신고하면 그룹에서도 자동으로 나가진다(팝업 문구가 그렇게 약속한다) — 나가기와 같은 자리로
     * 돌려보낸다.
     */
    private fun handleConfirmReportGroup() {
        if (state.value.visibleDialog != GroupSettingDialog.Report) return

        submitDialogAction(key = KEY_REPORT_GROUP, action = REPORT_ACTION) {
            reportGroup(groupId = groupId, reason = GROUP_REPORT_REASON)
        }
    }

    /**
     * 나가기와 신고는 결과가 같다 — 둘 다 이 그룹에서 빠져나온다. 다른 것은 부르는 API 와
     * 실패 로그의 이름뿐이라 한 자리에 모은다.
     *
     * 팝업은 먼저 닫는다. 왕복 동안은 스캐폴드가 화면을 덮고, 실패는 토스트가 말한다 —
     * 팝업을 띄운 채로 두면 그 덮개 아래 가려 아무것도 알리지 못한다.
     */
    private fun submitDialogAction(
        key: String,
        action: String,
        request: suspend () -> Result<*>,
    ) {
        updateState { copy(visibleDialog = null, isSubmittingDialogAction = true) }

        launch(key = key, onError = { onDialogActionFailed(it, action) }) {
            try {
                request()
                    .onSuccess { postSideEffect(GroupSettingSideEffect.NavigateToGroupList) }
                    .onFailure { onDialogActionFailed(it, action) }
            } finally {
                // 예외·취소 어느 경로로 빠져나가도 로딩이 걸린 채 남지 않게 한다
                updateState { copy(isSubmittingDialogAction = false) }
            }
        }
    }

    private fun onDialogActionFailed(
        throwable: Throwable,
        action: String,
    ) {
        viewModelLogger.e(throwable) { "${action}에 실패했다 - groupId: ${groupId.value}" }
        postSideEffect(GroupSettingSideEffect.ShowError(throwable.toGroupSettingError()))
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
        const val KEY_LEAVE_GROUP = "leaveGroup"
        const val KEY_REPORT_GROUP = "reportGroup"

        const val LEAVE_ACTION = "그룹 나가기"
        const val REPORT_ACTION = "그룹 신고"
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

// 초대 코드 복사 후 "복사됨" 문구가 원래 문구로 되돌아가기까지의 지연(ms)
private const val COPY_CODE_RESET_DELAY_MS = 2000L

// TODO: 사유를 고르는 UI 가 아직 없다. 서버는 사유를 필수로 받으므로(빈 값이면
//  400 INVALID_GROUP_REPORT_REASON) 화면이 대신 채운다 — 사유 선택이 생기면 이 상수는 사라진다.
private const val GROUP_REPORT_REASON = "그룹 설정에서 신고"

/**
 * 서버가 배정한 칩을 화면 색으로 옮긴다.
 *
 * 값이 없거나 반납된 자리는 [YGColorChipType.Default] 다 — 색이 "그룹 안의 이 사람"을 가리키는
 * 신호라, 가리킬 사람이 없을 때 아무 색이나 돌리면 신호가 거짓이 된다.
 */
internal fun NametagChipType?.toColorChipType(): YGColorChipType = when (this) {
    NametagChipType.TYPE1 -> YGColorChipType.NametagChip1
    NametagChipType.TYPE2 -> YGColorChipType.NametagChip2
    NametagChipType.TYPE3 -> YGColorChipType.NametagChip3
    NametagChipType.TYPE4 -> YGColorChipType.NametagChip4
    NametagChipType.TYPE5 -> YGColorChipType.NametagChip5
    NametagChipType.TYPE6 -> YGColorChipType.NametagChip6
    NametagChipType.TYPE7 -> YGColorChipType.NametagChip7
    NametagChipType.TYPE8 -> YGColorChipType.NametagChip8
    NametagChipType.TYPE9 -> YGColorChipType.NametagChip9
    NametagChipType.TYPE10 -> YGColorChipType.NametagChip10
    NametagChipType.TYPE11 -> YGColorChipType.NametagChip11
    NametagChipType.TYPE12 -> YGColorChipType.NametagChip12
    NametagChipType.RELEASED, null -> YGColorChipType.Default
}
