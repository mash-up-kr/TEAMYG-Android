package com.teamyg.parfait.core.designsystem.component.etc

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
    @DrawableRes trailingIcon: Int,
    onClickTrailingIcon: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = YGAtomicColors.Gray.Gray800,
) = YGListItemImpl(
    text = text,
    textColor = textColor,
    modifier = modifier,
) {
    YGIconButton(
        iconResource = trailingIcon,
        size = YGIconButtonSize.SIZE_44,
        contentDescription = null,
        onClick = onClickTrailingIcon,
    )
}

@Composable
fun YGListItem(
    text: String,
    subText: String,
    modifier: Modifier = Modifier,
    textColor: Color = YGAtomicColors.Gray.Gray800,
    subTextColor: Color = YGAtomicColors.Gray.Gray400,
) = YGListItemImpl(
    text = text,
    textColor = textColor,
    modifier = modifier,
) {
    Text(
        text = subText,
        style = YGTheme.typography.body.b02SB,
        color = subTextColor,
    )
}

@Composable
private fun YGListItemImpl(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    val verticalPadding = YGTheme.layout.padding.padding2

    Row(
        modifier = modifier
            .fillMaxWidth()
            // 두 오버로드의 높이를 맞춘다. 아이콘 쪽은 YGIconButton 이 높이를 정하고 subText 쪽은
            // 텍스트 한 줄뿐이라, 그냥 두면 같은 목록 안에서 줄마다 높이가 달라진다.
            // 리터럴 대신 아이콘 쪽 높이를 그대로 계산해 토큰이 바뀌어도 따라오게 한다
            .heightIn(min = YGIconButtonSize.SIZE_44.containerSize + verticalPadding * 2)
            .padding(
                horizontal = YGTheme.layout.padding.padding7,
                vertical = verticalPadding,
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

        trailing()
    }
}

@YGPreview
@Composable
private fun YGListItemPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        Text("trailingIcon")

        YGListItem(
            text = "서비스 이용약관",
            trailingIcon = R.drawable.ic_caret_right,
            onClickTrailingIcon = {},
        )

        Text("sub")

        YGListItem(
            text = "서비스 이용약관",
            subText = "부가 설명 텍스트",
        )
    }
}
