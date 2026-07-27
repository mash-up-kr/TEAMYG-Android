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
import com.teamyg.parfait.core.util.android.extension.drawTooltipCornerTopRight
import com.teamyg.parfait.core.util.android.extension.withStyle

@Composable
internal fun GroupListTooltip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White)
            .drawTooltipCornerTopRight(
                borderColor = YGAtomicColors.Melon.Melon500,
                backgroundColor = YGAtomicColors.Gray.White,
                cornerWidth = 17.dp,
                cornerHeight = 16.dp,
                endPadding = 45.dp,
                borderWidth = (1.25).dp,
            )
            .border(
                width = (1.25).dp,
                color = YGAtomicColors.Melon.Melon500,
            )
            .padding(
                vertical = YGTheme.layout.padding.padding6,
                horizontal = YGTheme.layout.padding.padding9,
            ),
    ) {
        Text(
            text = buildAnnotatedString {
                append("여기를 눌러 ")
                withStyle(
                    textStyle = YGTheme.typography.body.b02B
                        .copy(color = YGAtomicColors.Melon.Melon600),
                ) {
                    append("새 그룹")
                }
                append("을 만들거나,\n친구에게 받은 초대코드로 ")
                withStyle(
                    textStyle = YGTheme.typography.body.b02B
                        .copy(color = YGAtomicColors.Melon.Melon600),
                ) {
                    append("그룹에 참여")
                }
                append("해 보세요.")
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
