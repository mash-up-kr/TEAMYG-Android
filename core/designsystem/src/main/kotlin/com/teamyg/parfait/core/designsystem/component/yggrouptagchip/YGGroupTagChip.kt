package com.teamyg.parfait.core.designsystem.component.yggrouptagchip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcolorchip.YGColorChipType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox

private const val MAX_TEXT_WIDTH = 80

@Composable
fun YGGroupTagChip(
    name: String,
    time: String,
    colorType: YGColorChipType,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
        modifier = modifier
            .background(color = YGAtomicColors.Transparency.Black75, shape = YGTheme.shapes.radius.round)
            .padding(
                top = YGTheme.layout.padding.padding2,
                end = YGTheme.layout.padding.padding5,
                bottom = YGTheme.layout.padding.padding2,
                start = YGTheme.layout.padding.padding5,
            ),
    ) {
        Text(
            text = name,
            style = YGTheme.typography.body.b02SB,
            color = YGAtomicColors.Gray.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = MAX_TEXT_WIDTH.dp)
        )
        Spacer(
            modifier = Modifier
                .size((1.25).dp)
                .background(color = YGAtomicColors.Transparency.White50, shape = YGTheme.shapes.radius.round),
        )
        Text(
            text = time,
            style = YGTheme.typography.caption.c01R,
            color = colorType.textColor,
        )
    }
}

@Preview
@Composable
private fun YGGroupTagChipPreview() = PreviewBox {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        YGGroupTagChip(
            name = "매시업",
            time = "3분전",
            colorType = YGColorChipType.NametagChip1,
        )
        YGGroupTagChip(
            name = "매시업매시업매매",
            time = "3분전",
            colorType = YGColorChipType.NametagChip2,
        )
    }
}
