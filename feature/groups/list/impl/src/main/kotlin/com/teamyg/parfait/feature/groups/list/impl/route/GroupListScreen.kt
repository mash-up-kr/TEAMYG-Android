package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroup
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingGroupType
import com.teamyg.parfait.core.designsystem.component.ygtoppinggroup.YGToppingImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListParfaitLayout
import com.teamyg.parfait.feature.groups.list.impl.route.component.GroupListTooltip
import com.teamyg.parfait.feature.groups.list.impl.route.component.ToppingLayout
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

private const val SPECIAL_RULE_THRESHOLD = 3

// ToppingLayout 이 index 짝/홀로 좌우를 번갈아 배치하므로 LEFT/RIGHT 가 교차하도록 나열한다
private val TOPPING_PLACEMENT_TYPES = listOf(
    YGToppingGroupType.TYPE_1_LEFT,
    YGToppingGroupType.TYPE_1_RIGHT,
    YGToppingGroupType.TYPE_2_LEFT,
    YGToppingGroupType.TYPE_2_RIGHT,
    YGToppingGroupType.TYPE_3_LEFT,
    YGToppingGroupType.TYPE_3_RIGHT,
)

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

            LazyColumn(
                contentPadding = PaddingValues(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    bottom = YGTheme.layout.padding.padding6,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                item {
                    YGDate(
                        date = uiState.dateString,
                        day = uiState.dayOfWeekString,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))

                    GroupListContent(
                        groupList = uiState.groupList,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GroupListContent(
    groupList: List<MockToppingGroup>,
    modifier: Modifier = Modifier,
) {
    GroupListParfaitLayout(
        cherrySection = {
            Image(
                painter = painterResource(R.drawable.parfait_cherry),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(83.dp),
            )
        },
        topSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_top),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(210.dp),
            )
        },
        middleSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cream_default),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(241.dp),
            )
        },
        bottomSection = {
            Image(
                painter = painterResource(R.drawable.parfait_cup),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.width(324.dp),
            )
        },
        modifier = modifier,
    ) {
        ToppingLayout(
            contentPadding = PaddingValues(
                top = if (groupList.size <= SPECIAL_RULE_THRESHOLD) 108.dp else 96.dp,
                end = YGTheme.layout.padding.padding2,
                start = YGTheme.layout.padding.padding2,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            groupList.fastForEachIndexed { index, toppingGroup ->
                YGToppingGroup(
                    image = YGToppingImage.Remote(toppingGroup.imageUrl),
                    name = toppingGroup.name,
                    timestamp = toppingGroup.lastModify,
                    chipType = toppingGroup.chipType,
                    type = TOPPING_PLACEMENT_TYPES[index % TOPPING_PLACEMENT_TYPES.size],
                )
            }
        }
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
