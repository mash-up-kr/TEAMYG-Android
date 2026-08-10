package com.teamyg.parfait.feature.groups.list.impl.route

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.util.android.clickable.clickableYG
import com.teamyg.parfait.feature.groups.list.impl.R
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
                top = YGTheme.layout.padding.padding3,
                end = YGTheme.layout.padding.padding7,
            ),
    ) {
        // 아래 깔린 탑바의 칩과 정확히 겹치도록, 탑바가 칩을 세로 중앙 정렬하는 아이콘 버튼 높이의 행을 그대로 재현한다.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(YGIconButtonSize.SIZE_44.containerSize),
        ) {
            YGChipButton(
                text = stringResource(R.string.group_add),
                colors = YGChipButtonColorsDefaults.GrayOutline,
                onClick = {},
                startIconResource = DesignSystemR.drawable.ic_plus,
            )
        }

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(YGTheme.shapes.radius.medium1)
                .background(
                    color = YGAtomicColors.Cherry.Cherry50,
                    shape = YGTheme.shapes.radius.medium1,
                ),
        ) {
            YGActionItem(
                text = stringResource(R.string.group_create),
                iconResource = DesignSystemR.drawable.ic_new_group,
                onClick = onClickCreateNewGroup,
            )
            YGHorizontalDivider(modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding6))
            YGActionItem(
                text = stringResource(R.string.group_enter),
                iconResource = DesignSystemR.drawable.ic_enter,
                onClick = onClickEnterNewGroup,
            )
        }
    }
}

@Preview
@Composable
private fun GroupListAddGroupScreenPreview() = PreviewBox {
    GroupListAddGroupScreen(
        onClickCreateNewGroup = {},
        onClickEnterNewGroup = {},
        onDismissed = {},
    )
}
