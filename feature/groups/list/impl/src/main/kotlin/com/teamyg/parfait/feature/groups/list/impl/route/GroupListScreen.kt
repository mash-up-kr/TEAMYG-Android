package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty

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
            YGTopBarEmpty(
                onIconClick = {},
                rightContent = {
                    YGChipButton(
                        text = "그룹 추가하기",
                        colors = YGChipButtonColorsDefaults.CherryBackgroundPressed,
                        onClick = {},
                        startIconResource = R.drawable.ic_plus,
                    )
                },
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
