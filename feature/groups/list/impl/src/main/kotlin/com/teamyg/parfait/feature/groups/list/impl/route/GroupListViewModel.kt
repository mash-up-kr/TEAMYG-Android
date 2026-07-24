package com.teamyg.parfait.feature.groups.list.impl.route

import com.teamyg.parfait.core.ui.BaseViewModel
import com.teamyg.parfait.core.ui.UiIntent
import com.teamyg.parfait.core.ui.UiSideEffect
import com.teamyg.parfait.core.ui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class GroupListUiState(
    val groupList: List<String> = emptyList(), // Todo : 임시로 String 으로 설정하였습니다
    val groupAddButtonSelected: Boolean = false,
    val isTooltipVisible: Boolean = false,
) : UiState

sealed interface GroupListIntent : UiIntent {
    data object ClickTopBarChip : GroupListIntent
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
    override fun processIntent(intent: GroupListIntent) {
        when (intent) {
            GroupListIntent.ClickTopBarChip -> {
                updateState { copy(groupAddButtonSelected = true) }
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
