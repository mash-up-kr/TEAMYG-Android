package com.teamyg.parfait.feature.groups.list.impl.route.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.util.android.extension.drawTooltipCornerTop
import com.teamyg.parfait.core.util.android.extension.withStyle

@Composable
internal fun GroupListTooltip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White)
            .drawTooltipCornerTop(
                borderColor = YGAtomicColors.Soda.Soda500,
                backgroundColor = YGAtomicColors.Gray.White,
                cornerWidth = 17.dp,
                cornerHeight = 16.dp,
                arrowCenterX = { size.width - 45.dp.toPx() },
                borderWidth = (1.25).dp,
            ).border(
                width = (1.25).dp,
                color = YGAtomicColors.Soda.Soda500,
            ).padding(
                vertical = YGTheme.layout.padding.padding6,
                horizontal = YGTheme.layout.padding.padding9,
            ),
    ) {
        val emphasisStyle = YGTheme.typography.body.b02B
            .copy(color = YGAtomicColors.Soda.Soda500)
        Text(
            text = buildAnnotatedString {
                withStyle(textStyle = emphasisStyle) { append("새 그룹") }
                append("을 만들거나 ")
                withStyle(textStyle = emphasisStyle) { append("그룹에 참여") }
                append("하면\n내 그룹 목록을 ")
                withStyle(textStyle = emphasisStyle) { append("파르페") }
                append("로 쌓을 수 있어요.")
            },
            style = YGTheme.typography.body.b02R,
            color = YGAtomicColors.Gray.Black,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun GroupListTooltipPreview() = PreviewBox {
    GroupListTooltip()
}
