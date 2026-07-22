package com.teamyg.parfait.core.designsystem.component.ygtext

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
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = YGTheme.typography.body.b02R,
        color = YGAtomicColors.Gray.Gray600,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@YGPreview
@Composable
private fun YGDatePreview() = PreviewBox {
    YGDate(text = "7월 14일의 파르페")
}
