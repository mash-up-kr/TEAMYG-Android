package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListPullToRefreshBox
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListTopBar

private val ERROR_CONTENT_TOP_PADDING = 112.dp
private val ERROR_DESCRIPTION_TO_CUP_GAP = 234.dp
private val ERROR_CUP_WIDTH = 324.dp

@Composable
internal fun GroupListErrorScreen(
    uiState: GroupListUiState,
    onClickChip: () -> Unit,
    onClickSideMenu: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        GroupListTopBar(
            date = uiState.dateString,
            day = uiState.dayOfWeekString,
            onClickChip = onClickChip,
            onClickSideMenu = onClickSideMenu,
            // 불러오기에 실패한 상태에서는 그룹 추가 툴팁을 띄우지 않는다.
            isTooltipVisible = false,
        )

        GroupListPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
        ) {
            LazyColumn(
                contentPadding = PaddingValues(top = ERROR_CONTENT_TOP_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.group_list_error),
                        style = YGTheme.typography.title.t03SB,
                        color = YGAtomicColors.Gray.Gray500,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(ERROR_DESCRIPTION_TO_CUP_GAP))

                    Image(
                        painter = painterResource(R.drawable.parfait_cup),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.width(ERROR_CUP_WIDTH),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GroupListErrorScreenPreview() = PreviewBox {
    GroupListErrorScreen(
        uiState = GroupListUiState(
            isError = true,
            dateString = "July 26",
            dayOfWeekString = "Wed",
        ),
        onClickChip = {},
        onClickSideMenu = {},
        onRefresh = {},
    )
}
