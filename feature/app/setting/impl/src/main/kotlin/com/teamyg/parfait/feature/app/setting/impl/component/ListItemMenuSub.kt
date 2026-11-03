package com.teamyg.parfait.feature.app.setting.impl.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.app.setting.impl.R
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun ListItemMenuSub(
    title: String,
    subText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes trailingIcon: Int = DesignSystemR.drawable.ic_caret_right,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YGTheme.layout.padding.padding5),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // fill = false: 제목이 짧으면 배지가 바로 옆에 붙고, 길어지면 배지가 밀려나지
                // 않도록 제목이 남는 폭 안에서만 늘어난다
                Text(
                    text = title,
                    style = YGTheme.typography.body.b02SB,
                    color = YGAtomicColors.Gray.Gray800,
                    modifier = Modifier.weight(1f, fill = false),
                )
                NewBadge()
            }
            Text(
                text = subText,
                style = YGTheme.typography.caption.c01R,
                color = YGAtomicColors.Gray.Gray400,
            )
        }

        YGIconButton(
            iconResource = trailingIcon,
            size = YGIconButtonSize.SIZE_44,
            contentDescription = null,
            onClick = onClick,
        )
    }
}

@Composable
private fun NewBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.setting_badge_new),
        style = YGTheme.typography.caption.c01M,
        color = YGAtomicColors.Cherry.Cherry700,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(
                color = YGAtomicColors.Cherry.Cherry100,
                shape = YGTheme.shapes.radius.xSmall,
            ).padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@YGPreview
@Composable
private fun ListItemMenuSubPreview() = PreviewBox {
    Column {
        ListItemMenuSub(
            title = "알림 시간대",
            subText = "오전 9시 - 오후 9시",
            onClick = {},
        )
        ListItemMenuSub(
            title = "아주 아주 길어서 한 줄에 다 들어가지 않는 알림 설정 제목",
            subText = "오전 9시 - 오후 9시",
            onClick = {},
        )
    }
}
