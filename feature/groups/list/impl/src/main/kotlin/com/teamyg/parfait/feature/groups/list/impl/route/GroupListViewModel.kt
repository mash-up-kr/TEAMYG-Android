package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.ui.viewModelLogger
import com.teamyg.parfait.core.util.jvm.model.DateFormat
import com.teamyg.parfait.domain.model.error.AppError
import com.teamyg.parfait.domain.model.group.MyParfaitGroupVO
import com.teamyg.parfait.domain.model.id.GroupId
import com.teamyg.parfait.domain.usecase.group.GetMyGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

data class GroupListUiState(
    val groupList: List<MyParfaitGroupVO> = emptyList(),
    // Todo : 서버에서 내 닉네임을 받아오도록 변경 필요, 지금은 mock 값입니다
    val nickName: String = "모카",
    val groupAddButtonSelected: Boolean = false,
    val isTooltipVisible: Boolean = false,
    val isError: Boolean = false,
    val isRefreshing: Boolean = false,
    val dateString: String = "",
    val dayOfWeekString: String = "",
) : UiState

sealed interface GroupListIntent : UiIntent {
    /**
     * 화면이 앞에 섰다. 처음 열릴 때뿐 아니라 다른 화면에서 **돌아올 때마다** 온다.
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

    data object NavigateToCreateGroup : GroupListSideEffect

    data object NavigateToInviteCode : GroupListSideEffect
}

@HiltViewModel
class GroupListViewModel
@Inject
constructor(
    private val getMyGroups: GetMyGroupsUseCase,
) : BaseViewModel<GroupListUiState, GroupListIntent, GroupListSideEffect>(
    initialState = GroupListUiState(),
) {
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
                postSideEffect(GroupListSideEffect.NavigateToCreateGroup)
            }

            GroupListIntent.ClickEnterNewGroup -> {
                postSideEffect(GroupListSideEffect.NavigateToInviteCode)
            }

            GroupListIntent.Refresh -> loadGroups(isRefresh = true)
        }
    }

    /** 앱을 켜 둔 채 자정을 넘겨도 헤더가 어제에 머물지 않도록, 화면에 설 때마다 다시 센다 */
    private fun updateToday() {
        val today = Clock.System
            .now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
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
                getMyGroups()
                    .onSuccess { groups -> updateState { copy(groupList = groups, isError = false) } }
                    .onFailure(::handleLoadFailure)
            } finally {
                updateState { copy(isRefreshing = false) }
            }
        }
    }

    /**
     * 목록이 남아 있어도 에러 화면으로 넘긴다 — 실패를 알릴 다른 자리가 없어서, 낡은 목록을
     * 그대로 두면 사용자는 새로고침이 실패한 것을 알 방법이 없다.
     */
    private fun handleLoadFailure(throwable: Throwable) {
        updateState { copy(isError = true) }

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
