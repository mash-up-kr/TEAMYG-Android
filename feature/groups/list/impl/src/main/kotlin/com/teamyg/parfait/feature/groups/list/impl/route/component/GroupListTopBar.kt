package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButton
import com.teamyg.parfait.core.designsystem.component.ygchipbutton.YGChipButtonColorsDefaults
import com.teamyg.parfait.core.designsystem.component.ygtopbar.YGTopBarEmpty
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.feature.groups.list.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 그룹 리스트와 에러 화면이 공유하는 상단 바.
 *
 * pull-to-refresh 대상이 아니므로 두 화면 모두 스크롤 영역 바깥에 배치한다.
 *
 * @param onClickAddGroup null 이면 그룹 추가 칩을 감춘다.
 */
@Composable
internal fun GroupListTopBar(
    count: Int?,
    onClickSideMenu: () -> Unit,
    modifier: Modifier = Modifier,
    onClickAddGroup: (() -> Unit)? = null,
) {
    YGTopBarEmpty(
        title = stringResource(R.string.group_list_title),
        count = count?.toString(),
        onIconClick = onClickSideMenu,
        modifier = modifier,
        rightContent = {
            if (onClickAddGroup != null) {
                YGChipButton(
                    text = stringResource(R.string.group_add),
                    colors = YGChipButtonColorsDefaults.GrayOutline,
                    onClick = onClickAddGroup,
                    startIconResource = DesignSystemR.drawable.ic_plus,
                )
            }
        },
    )
}

@Preview
@Composable
private fun GroupListTopBarPreview() = PreviewBox {
    GroupListTopBar(
        count = 3,
        onClickSideMenu = {},
        onClickAddGroup = {},
    )
}

@Preview
@Composable
private fun GroupListTopBarZeroCountPreview() = PreviewBox {
    GroupListTopBar(
        count = 0,
        onClickSideMenu = {},
        onClickAddGroup = {},
    )
}

@Preview
@Composable
private fun GroupListTopBarWithoutChipPreview() = PreviewBox {
    GroupListTopBar(
        count = null,
        onClickSideMenu = {},
    )
}
