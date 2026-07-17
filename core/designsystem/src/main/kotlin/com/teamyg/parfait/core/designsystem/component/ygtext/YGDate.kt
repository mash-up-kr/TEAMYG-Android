package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors


@Composable
fun YGDate(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = YGTheme.typography.body.b02R,
        color = YGAtomicColors.Gray.Gray600,
        modifier = modifier.padding(
            vertical = YGTheme.layout.padding.padding5,
            horizontal = YGTheme.layout.padding.padding3
        ),
    )
}

@Preview
@Composable
private fun YGDatePreview() {
    YGCustomTheme {
        YGDate(text = "7월 14일의 파르페")
    }
}
