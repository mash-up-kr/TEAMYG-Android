package com.teamyg.parfait.core.designsystem.component.etc

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGListItem(
    text: String,
    modifier: Modifier = Modifier,
    subText: String? = null,
    @DrawableRes trailingIcon: Int? = null,
    textColor: Color = YGAtomicColors.Gray.Gray800,
    subTextColor: Color = YGAtomicColors.Gray.Gray400,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = YGTheme.layout.padding.padding7,
                vertical = YGTheme.layout.padding.padding2,
            ),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = YGTheme.typography.body.b02R,
            color = textColor,
            modifier = Modifier.weight(1f),
        )

        subText?.let {
            Text(
                text = subText,
                style = YGTheme.typography.body.b02SB,
                color = subTextColor,
            )
        }

        trailingIcon?.let {
            YGIconButton(
                iconResource = trailingIcon,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClick,
            )
        }
    }
}

@YGPreview
@Composable
private fun YGListItemPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("trailingIcon + no sub")

        YGListItem(
            text = "서비스 이용약관",
            trailingIcon = R.drawable.ic_caret_right,
            onClick = {},
        )

        Text("sub")

        YGListItem(
            text = "서비스 이용약관",
            subText = "부가 설명 텍스트",
            onClick = {},
        )

        Text("no trailingIcon")

        YGListItem(
            text = "서비스 이용약관",
            onClick = {},
        )
    }
}
