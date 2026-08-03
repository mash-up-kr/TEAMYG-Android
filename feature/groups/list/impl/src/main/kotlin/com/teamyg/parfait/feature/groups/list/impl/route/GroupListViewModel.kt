package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.designsystem.component.yggrouptagchip.YGGrouptagChipType
import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import com.teamyg.parfait.core.util.jvm.model.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject
import kotlin.time.Clock

data class MockToppingGroup(
    val name: String,
    val imageUrl: String,
    val lastModify: String,
) {
    val chipType: YGGrouptagChipType = YGGrouptagChipType.TYPE_1_2
}

data class GroupListUiState(
    // Todo : 임시로 MockToppingGroup 으로 설정하였습니다
    val groupList: List<MockToppingGroup> = listOf(
        MockToppingGroup(
            "매시업1",
            "https://pds.joongang.co.kr/news/component/htmlphoto_mmdata/201611/16/htm_2016111623638229254.png",
            "3분전",
        ),
        MockToppingGroup(
            "매시업매시업매시업",
            "https://i.pinimg.com/236x/d8/a6/cb/d8a6cbb02bc2c5c27ae238db2e89425d.jpg",
            "10분전",
        ),
        MockToppingGroup(
            "매시업3",
            "https://i.namu.wiki/i/izVXkClWRy9-s5DAkC_lGo3za4Zy9seGH1V6AM0qZJzsckE9eWe6-Hp-1OvJm_DkVv7BL7U0Ar7QB89ApaklkQ.webp",
            "1시간전",
        ),
        MockToppingGroup(
            "매시업4",
            "https://https://i.namu.wiki/i/h01cpKjF51MCkPZBi9-x7RMltpQbztoVgRB_0fnqrGA6S0M_9mcxpptqAmhr1YxCo0fWeErT3DGg55Nb8O2WeA.webp",
            "1시간전",
        ),
    ),
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
