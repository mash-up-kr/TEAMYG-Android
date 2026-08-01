package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGDate(
    date: String,
    day: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color = YGAtomicColors.Gray.White)
            .border(width = 0.75.dp, color = YGAtomicColors.Gray.Gray800)
            .background(color = YGAtomicColors.Gray.White)
            .padding(
                vertical = YGTheme.layout.padding.padding3,
                horizontal = YGTheme.layout.padding.padding4,
            ),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap3),
    ) {
        Text(
            text = date,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
        )
        Text(
            text = "($day)",
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray300,
        )
    }
}

@YGPreview
@Composable
private fun YGDatePreview() = PreviewBox {
    YGDate(
        date = "December 31",
        day = "Wed",
    )
}
