package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.extension.drawTooltipCornerTopRight
import com.teamyg.parfait.core.util.android.extension.withStyle
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen(
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
                            GroupListTooltip()
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
                    // Todo : 임시코드
                    Spacer(modifier = Modifier.height(24.dp))
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
            }
        }
    }
}

@Composable
private fun GroupListTooltip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .background(color = YGAtomicColors.Gray.White)
            .drawTooltipCornerTopRight(
                borderColor = YGAtomicColors.Melon.Melon500,
                backgroundColor = YGAtomicColors.Gray.White,
                cornerWidth = 17.dp,
                cornerHeight = 16.dp,
                endPadding = 45.dp,
            ).border(
                width = (1.25).dp,
                color = YGAtomicColors.Melon.Melon500,
            ).padding(
                vertical = YGTheme.layout.padding.padding6,
                horizontal = YGTheme.layout.padding.padding9,
            ),
    ) {
        Text(
            text = buildAnnotatedString {
                append("여기를 눌러 ")
                withStyle(
                    textStyle = YGTheme.typography.body.b02B
                        .copy(color = YGAtomicColors.Melon.Melon600),
                ) {
                    append("새 그룹")
                }
                append("을 만들거나,\n친구에게 받은 초대코드로 ")
                withStyle(
                    textStyle = YGTheme.typography.body.b02B
                        .copy(color = YGAtomicColors.Melon.Melon600),
                ) {
                    append("그룹에 참여")
                }
                append("해 보세요.")
            },
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Black,
            textAlign = TextAlign.Center,
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
