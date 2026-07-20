package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.border
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
        modifier = Modifier
            .border(width = 0.75.dp, color = YGAtomicColors.Gray.Gray800)
            .padding(
                vertical = YGTheme.layout.padding.padding3,
                horizontal = YGTheme.layout.padding.padding4,
            ),
    ) {
        Text(
            text = date,
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray800,
        )
        Text(
            text = "(" + day + ")",
            style = YGTheme.typography.body.b01R,
            color = YGAtomicColors.Gray.Gray300,
            modifier = modifier.padding(
                start = YGTheme.layout.padding.padding3,
            ),
        )
    }
}

@YGPreview
@Composable
private fun YGDatePreview() {
    YGCustomTheme {
        YGDate(
            date = "December 31",
            day = "Wed",
        )
    }
}
