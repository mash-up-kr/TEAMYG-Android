package com.teamyg.parfait.core.designsystem.component.ygtext

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

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

@YGPreview
@Composable
private fun YGLabelPreview() = PreviewBox {
    YGLabel(text = "레이블")
}
