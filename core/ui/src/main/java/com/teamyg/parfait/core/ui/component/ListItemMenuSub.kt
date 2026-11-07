package com.teamyg.parfait.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygchipnew.YGChipNew
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple

/**
 * 가로 여백은 두지 않는다 — 놓이는 화면이 정한다.
 */
@Composable
fun ListItemMenuSub(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subText: String? = null,
    isNew: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickableYGNoRipple(onClick = onClick)
            .padding(vertical = YGTheme.layout.padding.padding5),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // fill = false: 제목이 짧으면 칩이 바로 옆에 붙고, 길어지면 칩이 밀려나지 않도록
                // 제목이 남는 폭 안에서만 늘어난다
                Text(
                    text = title,
                    style = YGTheme.typography.body.b02SB,
                    color = YGAtomicColors.Gray.Gray800,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )

                if (isNew) {
                    YGChipNew()
                }
            }

            if (subText != null) {
                Text(
                    text = subText,
                    style = YGTheme.typography.caption.c01R,
                    color = YGAtomicColors.Gray.Gray400,
                )
            }
        }

        YGIconButton(
            iconResource = DesignSystemR.drawable.ic_caret_right,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClick,
        )
    }
}

@YGPreview
@Composable
private fun ListItemMenuSubPreview() = PreviewBox {
    Column(modifier = Modifier.padding(horizontal = YGTheme.layout.padding.padding7)) {
        ListItemMenuSub(
            title = "파르페 v1.5.0 업데이트 안내",
            subText = "2026.11.12",
            onClick = {},
        )
        ListItemMenuSub(
            title = "공지사항",
            onClick = {},
            isNew = true,
        )
        ListItemMenuSub(
            title = "이용약관 및 개인정보처리방침 개정 안내",
            subText = "2026.11.17",
            onClick = {},
            isNew = true,
        )
        ListItemMenuSub(
            title = "아주 긴 제목의 공지사항이 두 줄을 넘어가는 경우에 말줄임이 들어가고 칩은 밀려나지 않아야 하는 예시 제목입니다 정말로 길어요",
            subText = "2026.11.09",
            onClick = {},
            isNew = true,
        )
    }
}
