package com.teamyg.parfait.feature.groups.canvas.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.teamyg.parfait.core.designsystem.component.etc.YGHorizontalDivider
import com.teamyg.parfait.core.designsystem.component.ygactionitem.YGActionItem
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.feature.groups.canvas.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

/**
 * 혼자 공간 햄버거가 여는 비우기·삭제하기 메뉴. Figma 시안이 없어 그룹 목록의 "그룹 추가하기"
 * 드롭다운과 같은 모양으로 둔다. 딤은 부르는 쪽이 깐다 — 빈 자리를 누르면 [onDismiss].
 *
 * 탑바(`YGTopBarCanvas`)의 안쪽 여백과 같은 자리에 햄버거를 다시 그려, 딤 위에서도 누른 버튼이
 * 그대로 보이고 그 바로 아래로 메뉴가 펼쳐진다.
 */
@Composable
internal fun SoloSpaceMenu(
    onClickReset: () -> Unit,
    onClickDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap4),
        modifier = modifier
            .fillMaxSize()
            .clickableYGNoRipple(onClick = onDismiss)
            .padding(
                top = YGTheme.layout.padding.padding3,
                end = YGTheme.layout.padding.padding3,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(YGIconButtonSize.SIZE_44.containerSize),
        ) {
            YGIconButton(
                iconResource = DesignSystemR.drawable.ic_hamburger,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = stringResource(R.string.solo_canvas_menu_close),
                onClick = onDismiss,
            )
        }

        Column(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .clip(YGTheme.shapes.radius.medium1)
                .background(
                    color = YGAtomicColors.Gray.White,
                    shape = YGTheme.shapes.radius.medium1,
                ),
        ) {
            YGActionItem(
                text = stringResource(R.string.solo_canvas_menu_reset),
                onClick = onClickReset,
            )
            YGHorizontalDivider(modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding6))
            YGActionItem(
                text = stringResource(R.string.solo_canvas_menu_delete),
                onClick = onClickDelete,
            )
        }
    }
}

@YGPreview
@Composable
private fun SoloSpaceMenuPreview() = PreviewBox {
    SoloSpaceMenu(
        onClickReset = {},
        onClickDelete = {},
        onDismiss = {},
        modifier = Modifier.background(YGAtomicColors.Transparency.Black25),
    )
}
