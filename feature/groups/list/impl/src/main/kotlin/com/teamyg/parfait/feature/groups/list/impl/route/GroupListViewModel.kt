package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.lifecycle.viewModelScope
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.model.DateFormat
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsFlowUseCase
import com.teamyg.parfait.domain.usecase.group.RefreshMyGroupsUseCase
import com.teamyg.parfait.domain.usecase.member.GetMyAccountFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import javax.inject.Inject

data class GroupListUiState(
    /** `null` 은 아직 한 번도 받지 못했다는 뜻. 0건과 구분한다 */
    val groupList: List<MyParfaitGroupVO>? = null,
    val nickName: String? = null,
    val groupAddButtonSelected: Boolean = false,
    val isTooltipVisible: Boolean = false,
    val isError: Boolean = false,
    val isRefreshing: Boolean = false,
    val dateString: String = "",
    val dayOfWeekString: String = "",
) : UiState

sealed interface GroupListIntent : UiIntent {
    /**
     * 화면이 앞에 섰다. 처음 열릴 때뿐 아니라 다른 화면에서 **돌아올 때마다** 온다 — ViewModel 은
     * NavEntry 가 백스택에 남아 있는 한 살아 있어, 초기화 한 번으로는 목록이 낡는다.
     *
     * 목록의 최근 사진은 다른 멤버가 올려도 바뀌므로, 내 앱 안의 변경만 좇아서는 최신이 될 수
     * 없다. 들어올 때마다 다시 물어보는 것이 최신을 보장하는 유일한 방법이다.
     */
    data object Enter : GroupListIntent

    data object ClickTopBarChip : GroupListIntent

    data object DismissedTopBarChip : GroupListIntent

    data object ClickCreateNewGroup : GroupListIntent

    data object ClickEnterNewGroup : GroupListIntent

    data object ClickSideMenu : GroupListIntent

    data class ClickTopping(val groupId: GroupId) : GroupListIntent

    data object Refresh : GroupListIntent
}

sealed interface GroupListSideEffect : UiSideEffect {
    data object NavigateToAppSideMenu : GroupListSideEffect

    data class NavigateToCanvas(val groupId: GroupId) : GroupListSideEffect

    /** 닉네임이 없으면 나가지 않는 이펙트라, 갈 화면이 받을 값을 실어 보낸다 */
    data class NavigateToCreateGroup(val nickName: String) : GroupListSideEffect

    /**
     * 당겨서 새로고침이 실패했는데 보여 줄 목록은 남아 있다.
     *
     * 화면은 그대로 두고 토스트로만 알린다 — 목록이 바뀌지 않은 것은 "받아 봤는데 새 소식이
     * 없다"와 구분되지 않아, 사용자가 직접 시킨 일이 실패했다는 것을 말해 줄 자리가 따로 필요하다.
     */
    data object ShowRefreshError : GroupListSideEffect

    data object NavigateToInviteCode : GroupListSideEffect
}

@HiltViewModel
class GroupListViewModel
@Inject
constructor(
    private val getMyGroupsFlow: GetMyGroupsFlowUseCase,
    private val refreshMyGroups: RefreshMyGroupsUseCase,
    private val getMyAccountFlow: GetMyAccountFlowUseCase,
) : BaseViewModel<GroupListUiState, GroupListIntent, GroupListSideEffect>(
    initialState = GroupListUiState(),
) {
    init {
        observeGroups()
        observeMyAccount()
    }

    /**
     * 표시는 캐시가 맡는다 — 다른 화면이 그룹을 만들거나 나가면 이 화면이 다시 조회하지 않아도
     * 그 자리에서 반영된다.
     */
    private fun observeGroups() {
        viewModelScope.launch {
            getMyGroupsFlow().collect { groups -> updateState { copy(groupList = groups) } }
        }
    }

    /**
     * 닉네임은 [GroupListIntent.Enter] 가 아니라 생성 시점에 구독한다 — 목록과 달리 계정
     * SSoT 가 밀어 주는 값이라 화면에 설 때마다 끌어올 이유가 없다. 구독만 하고 갱신은
     * 걸지 않는다 — 계정 갱신은 로그인/가입·스플래시·닉네임 변경 쪽이 맡는다.
     */
    private fun observeMyAccount() {
        launch {
            getMyAccountFlow().collect { account ->
                updateState { copy(nickName = account?.nickname?.value) }
            }
        }
    }

    override fun processIntent(intent: GroupListIntent) {
        when (intent) {
            GroupListIntent.Enter -> {
                updateToday()
                loadGroups(isRefresh = false)
            }

            GroupListIntent.ClickTopBarChip -> {
                updateState { copy(groupAddButtonSelected = true) }
            }

            GroupListIntent.DismissedTopBarChip -> {
                updateState { copy(groupAddButtonSelected = false) }
            }

            GroupListIntent.ClickSideMenu -> {
                postSideEffect(GroupListSideEffect.NavigateToAppSideMenu)
            }

            is GroupListIntent.ClickTopping -> {
                postSideEffect(GroupListSideEffect.NavigateToCanvas(intent.groupId))
            }

            GroupListIntent.ClickCreateNewGroup -> {
                closeAddGroup()
                handleClickCreateNewGroup()
            }

            GroupListIntent.ClickEnterNewGroup -> {
                closeAddGroup()
                postSideEffect(GroupListSideEffect.NavigateToInviteCode)
            }

            GroupListIntent.Refresh -> loadGroups(isRefresh = true)
        }
    }

    /**
     * 닉네임 없이 열면 사용자가 이미 가진 이름을 다시 입력하게 되므로, 스트림이 첫 값을
     * 내놓기 전에는 열지 않는다. DataStore 를 한 번 읽는 사이라 따로 알리지 않는다 —
     * 다시 누르면 열린다.
     */
    private fun handleClickCreateNewGroup() {
        val nickName = state.value.nickName

        if (nickName == null) {
            viewModelLogger.w { "닉네임이 아직 없어 그룹 만들기를 열지 않는다" }
            return
        }

        postSideEffect(GroupListSideEffect.NavigateToCreateGroup(nickName))
    }

    /**
     * ViewModel 은 NavEntry 가 백스택에 남아 있는 한 살아 있어, 오버레이를 켜 둔 채 나가면
     * 목록으로 돌아왔을 때 누른 적 없는 오버레이가 그대로 떠 있다. 뒤로 가기 한 번을 그걸
     * 닫는 데 쓰게 되는 셈이라, 나가는 길에 접는다.
     */
    private fun closeAddGroup() {
        updateState { copy(groupAddButtonSelected = false) }
    }

    /** 앱을 켜 둔 채 파르페 하루 경계를 넘겨도 헤더가 어제에 머물지 않도록, 화면에 설 때마다 다시 센다 */
    private fun updateToday() {
        val today = parfaitToday()
        updateState {
            copy(
                dateString = today.format(DateFormat.FullMonthWithDay),
                dayOfWeekString = today.format(DateFormat.AbbreviatedDayOfWeek),
            )
        }
    }

    /**
     * 새로고침 표시를 [launch] 밖에서 켜는 이유: 당겨서 새로고침이 [KEY_LOAD_GROUPS] 가드에
     * 막혀도 인디케이터는 돌아야 하고, 실제로도 조회가 돌고 있다.
     */
    private fun loadGroups(isRefresh: Boolean) {
        if (isRefresh) {
            updateState { copy(isRefreshing = true) }
        }

        launch(key = KEY_LOAD_GROUPS) {
            try {
                refreshMyGroups()
                    .onSuccess { updateState { copy(isError = false) } }
                    .onFailure { throwable -> handleLoadFailure(throwable, isRefresh) }
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 조회는 [GroupListIntent.Enter] 로 화면에 돌아올 때마다 나가므로, 실패마다 에러 화면으로
     * 넘기면 뒤로 온 것만으로 보던 목록이 통째로 사라진다. 낡아도 남겨 두는 편이 낫다.
     *
     * 대신 남겨 두면 실패가 화면에서 사라지므로, 사용자가 **직접 시킨** 새로고침이었을 때는
     * 토스트로 따로 알린다. 목록이 비어 에러 화면으로 넘어가는 경우는 그 화면이 이미 실패를
     * 말하고 있어 토스트를 겹치지 않는다.
     */
    private fun handleLoadFailure(
        throwable: Throwable,
        isRefresh: Boolean,
    ) {
        // 미조회(null)와 0건을 함께 "보여 줄 것이 없다"로 본다 — 둘 다 에러 화면이 맞다
        val hasList = state.value.groupList
            .isNullOrEmpty()
            .not()
        updateState { copy(isError = hasList.not()) }

        if (isRefresh && hasList) {
            postSideEffect(GroupListSideEffect.ShowRefreshError)
        }

        when (throwable) {
            is AppError.Network -> viewModelLogger.e(throwable) { "그룹 목록 조회 실패 — 네트워크 단절" }

            is AppError.Server ->
                viewModelLogger.e(throwable) { "그룹 목록 조회 실패 — 서버 에러 ${throwable.code}" }

            else -> viewModelLogger.e(throwable) { "그룹 목록 조회 실패 — 예상하지 못한 오류" }
        }
    }

    private companion object {
        const val KEY_LOAD_GROUPS = "loadGroups"
    }
}
