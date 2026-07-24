package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@OptIn(ExperimentalMaterial3Api::class)
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
                            Box(modifier = Modifier.background(color = Color.Red)) {
                                Text("툴팁입니다") // Todo : 툴팁 UI 구현
                            }
                        },
                        state = tooltipState,
                        onDismissRequest = { tooltipState.dismiss() },
                        focusable = true,
                    ) {
                        YGChipButton(
                            text = "그룹 추가하기", // Todo : core:ui 에 string resource 로 분리
                            colors = YGChipButtonColorsDefaults.CherryBackgroundPressed,
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
