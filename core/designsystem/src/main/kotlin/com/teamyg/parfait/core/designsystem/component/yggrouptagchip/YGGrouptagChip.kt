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
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.theme.size.SizeTokens
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private val DividerSize = 1.25.dp

/**
 * Figma Grouptag-Chip
 */
@Composable
fun YGGrouptagChip(
    name: String,
    timestamp: String,
    type: YGGrouptagChipType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(
                color = YGAtomicColors.Transparency.Black75,
                shape = YGTheme.shapes.radius.round,
            ).padding(
                horizontal = YGTheme.layout.padding.padding5,
                vertical = YGTheme.layout.padding.padding2,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap2),
    ) {
        Text(
            text = name,
            color = YGAtomicColors.Gray.White,
            style = YGTheme.typography.body.b02SB,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = SizeTokens.Size80.getDp()),
        )
        Spacer(
            modifier = Modifier
                .size(DividerSize)
                .background(
                    color = YGAtomicColors.Transparency.White50,
                    shape = YGTheme.shapes.radius.round,
                ),
        )
        Text(
            text = timestamp,
            color = type.timestampColor,
            style = YGTheme.typography.caption.c01R,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@YGPreview
@Composable
private fun YGGrouptagChipPreview() = PreviewBox {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        YGGrouptagChipType.entries.forEach { type ->
            YGGrouptagChip(
                name = "잠탈감금",
                timestamp = "3분전",
                type = type,
            )
        }
        YGGrouptagChip(
            name = "팀장은 진짜 연경이야",
            timestamp = "3분전",
            type = YGGrouptagChipType.TYPE_7_8,
        )
    }
}
