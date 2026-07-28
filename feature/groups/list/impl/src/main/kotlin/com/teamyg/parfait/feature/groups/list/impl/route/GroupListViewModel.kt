package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.jvm.model.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

data class GroupListUiState(
    val groupList: List<String> = emptyList(), // Todo : 임시로 String 으로 설정하였습니다
    val groupAddButtonSelected: Boolean = false,
    val isTooltipVisible: Boolean = false,
    val dateString: String = "",
    val dayOfWeekString: String = "",
) : UiState

sealed interface GroupListIntent : UiIntent {
    data object ClickTopBarChip : GroupListIntent

    data object DismissedTopBarChip : GroupListIntent

    data object ClickCreateNewGroup : GroupListIntent

    data object ClickEnterNewGroup : GroupListIntent

    data object ClickSideMenu : GroupListIntent

    data object ClickTopping : GroupListIntent
}

sealed interface GroupListSideEffect : UiSideEffect {
    data object NavigateToAppSideMenu : GroupListSideEffect

    data object NavigateToCanvas : GroupListSideEffect

    data object NavigateToCreateGroup : GroupListSideEffect

    data object NavigateToInviteCode : GroupListSideEffect
}

@HiltViewModel
class GroupListViewModel
@Inject
constructor() : BaseViewModel<GroupListUiState, GroupListIntent, GroupListSideEffect>(
    initialState = GroupListUiState(),
) {
    init {
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

    override fun processIntent(intent: GroupListIntent) {
        when (intent) {
            GroupListIntent.ClickTopBarChip -> {
                updateState { copy(groupAddButtonSelected = true) }
            }

            GroupListIntent.DismissedTopBarChip -> {
                updateState { copy(groupAddButtonSelected = false) }
            }

            GroupListIntent.ClickSideMenu -> {
                postSideEffect(GroupListSideEffect.NavigateToAppSideMenu)
            }

            GroupListIntent.ClickTopping -> {
                postSideEffect(GroupListSideEffect.NavigateToCanvas)
            }

            GroupListIntent.ClickCreateNewGroup -> {
                postSideEffect(GroupListSideEffect.NavigateToCreateGroup)
            }

            GroupListIntent.ClickEnterNewGroup -> {
                postSideEffect(GroupListSideEffect.NavigateToInviteCode)
            }
        }
    }
}
