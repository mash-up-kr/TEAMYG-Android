package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarDefault
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate

@Composable
fun GroupListScreen(
    uiState: GroupListUiState,
    onClickChip: () -> Unit,
    onClickCreateNewGroup: () -> Unit,
    onClickEnterNewGroup: () -> Unit,
    onClickSideMenu: () -> Unit,
    onClickTopping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column {
            YGTopBarDefault(
                onIconClick = onClickSideMenu,
                onChipClick = onClickChip,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    YGDate(
                        date = uiState.dateString,
                        day = uiState.dayOfWeekString,
                    )
                }
            }
        }
    }
}
