package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.teamyg.parfait.core.designsystem.theme.YGCustomTheme
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = YGTheme.typography.body.b02R,
        color = YGAtomicColors.Gray.Gray400,
        modifier = modifier,
    )
}

@Preview
@Composable
private fun YGLabelPreview() {
    YGCustomTheme {
        YGLabel(text = "레이블")
    }
}
