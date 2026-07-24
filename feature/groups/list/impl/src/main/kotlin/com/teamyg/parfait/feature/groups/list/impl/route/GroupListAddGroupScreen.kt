package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.util.android.clickable.clickableYG
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
fun GroupListAddGroupScreen(
    onClickCreateNewGroup: () -> Unit,
    onClickEnterNewGroup: () -> Unit,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier
            .clickableYG(onClick = onDismissed)
            .padding(
                start = YGTheme.layout.padding.padding7,
                top = 14.dp,
                end = YGTheme.layout.padding.padding7,
            ),
    ) {
        YGChipButton(
            text = "그룹 추가하기", // Todo : core:ui 에 string resource 로 분리
            colors = YGChipButtonColorsDefaults.CherryBackgroundPressed, // Todo@문의 : 디자인가이드와 다름 문의 필요
            onClick = {},
            startIconResource = DesignSystemR.drawable.ic_plus,
        )

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(YGTheme.shapes.radius.medium1)
                .background(
                    color = YGAtomicColors.Cherry.Cherry50,
                    shape = YGTheme.shapes.radius.medium1,
                )
                .padding(horizontal = YGTheme.layout.padding.padding5),
        ) {
            // Todo@문의 : 기존 디자인시스템 컴포넌트 업데이트가 필요하다고 공유하기
            YGActionItem(
                text = "그룹 만들기", // Todo : core:ui 에 string resource 로 분리
                onClick = onClickCreateNewGroup,
            )
            YGHorizontalDivider(modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding2))
            YGActionItem(
                text = "그룹 들어가기", // Todo : core:ui 에 string resource 로 분리
                onClick = onClickEnterNewGroup,
            )
        }
    }
}
