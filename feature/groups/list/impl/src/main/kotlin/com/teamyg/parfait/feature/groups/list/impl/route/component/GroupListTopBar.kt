package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 그룹 리스트와 에러 화면이 공유하는 상단 바.
 *
 * pull-to-refresh 대상이 아니므로 두 화면 모두 스크롤 영역 바깥에 배치한다.
 *
 * @param onClickAddGroup null 이면 그룹 추가 칩과 툴팁을 함께 감춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupListTopBar(
    date: String,
    day: String,
    onClickSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onClickAddGroup: (() -> Unit)? = null,
    isTooltipVisible: Boolean = false,
) {
    val showTooltip = onClickAddGroup != null && isTooltipVisible

    val tooltipState = rememberTooltipState(
        initialIsVisible = false,
        isPersistent = true,
    )

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            tooltipState.show()
        } else {
            tooltipState.dismiss()
        }
    }

    YGTopBarEmpty(
        date = date,
        day = day,
        onIconClick = onClickSideMenu,
        modifier = modifier,
        rightContent = {
            if (onClickAddGroup != null) {
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
                        text = stringResource(R.string.group_add),
                        colors = YGChipButtonColorsDefaults.GrayOutline,
                        onClick = onClickAddGroup,
                        startIconResource = DesignSystemR.drawable.ic_plus,
                    )
                }
            }
        },
    )
}
