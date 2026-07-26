package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListTooltip
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupListScreen(
    uiState: GroupListUiState,
    onClickChip: () -> Unit,
    onClickSideMenu: () -> Unit,
    onClickTopping: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tooltipState = rememberTooltipState(
        initialIsVisible = false,
        isPersistent = true,
    )

    LaunchedEffect(Unit) {
        tooltipState.show()
    }

    Box(modifier = modifier) {
        Column {
            YGTopBarEmpty(
                onIconClick = onClickSideMenu,
                rightContent = {
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            positioning = TooltipAnchorPosition.Below,
                            spacingBetweenTooltipAndAnchor = 15.dp,
                        ),
                        tooltip = {
                            GroupListTooltip(
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        },
                        state = tooltipState,
                        enableUserInput = false,
                        onDismissRequest = { tooltipState.dismiss() },
                        focusable = true,
                    ) {
                        YGChipButton(
                            text = "그룹 추가하기", // Todo : core:ui 에 string resource 로 분리
                            colors = YGChipButtonColorsDefaults.CherrySubtle,
                            onClick = onClickChip,
                            startIconResource = DesignSystemR.drawable.ic_plus,
                        )
                    }
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = YGTheme.layout.padding.padding7,
                        end = YGTheme.layout.padding.padding7,
                        bottom = YGTheme.layout.padding.padding6,
                    ),
            ) {
                YGDate(
                    date = uiState.dateString,
                    day = uiState.dayOfWeekString,
                )

                Spacer(modifier = Modifier.height(24.dp))

                GroupListContent()
            }
        }
    }
}

@Composable
fun GroupListContent(modifier: Modifier = Modifier) {
    // Todo : 임시코드
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            painter = painterResource(R.drawable.parfait_cherry),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(83.dp)
                .zIndex(0f),
        )
        Image(
            painter = painterResource(R.drawable.parfait_cream_top),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(210.dp)
                .offset(y = (-40).dp)
                .zIndex(0f),
        )
        Image(
            painter = painterResource(R.drawable.parfait_cream_default),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(241.dp)
                .offset(y = (-40 - 44).dp)
                .zIndex(-1f),
        )
        Image(
            painter = painterResource(R.drawable.parfait_cream_default),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(241.dp)
                .offset(y = (-40 - 44 - 66).dp)
                .zIndex(-2f),
        )
        Image(
            painter = painterResource(R.drawable.parfait_cream_default),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(241.dp)
                .offset(y = (-40 - 44 - 66 - 66).dp)
                .zIndex(-3f),
        )
        Image(
            painter = painterResource(R.drawable.parfait_cup),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .width(324.dp)
                .offset(y = (-40 - 44 - 66 - 66 - 32).dp)
                .zIndex(0f),
        )
    }
}

private class GroupListScreenPreviewParameterProvider :
    PreviewParameterProvider<GroupListUiState> {
    override val values: Sequence<GroupListUiState>
        get() = sequenceOf(
            GroupListUiState(
                groupAddButtonSelected = false,
                isTooltipVisible = false,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
            GroupListUiState(
                groupAddButtonSelected = true,
                isTooltipVisible = false,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
            GroupListUiState(
                groupAddButtonSelected = false,
                isTooltipVisible = true,
                dateString = "July 26",
                dayOfWeekString = "Wed",
            ),
        )
}

@YGPreview
@Composable
private fun GroupListScreenPreview(
    @PreviewParameter(GroupListScreenPreviewParameterProvider::class) uiState: GroupListUiState,
) = PreviewBox {
    GroupListScreen(
        uiState = uiState,
        onClickChip = {},
        onClickSideMenu = {},
        onClickTopping = {},
    )
}
