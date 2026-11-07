package com.teamyg.parfait.core.designsystem.component.ygchipnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

private const val NEW_TEXT = "NEW"

@Composable
fun YGChipNew(modifier: Modifier = Modifier) {
    Text(
        text = NEW_TEXT,
        style = YGTheme.typography.caption.c01M,
        color = YGAtomicColors.Cherry.Cherry700,
        modifier = modifier
            .background(color = YGAtomicColors.Cherry.Cherry100)
            .padding(
                horizontal = YGTheme.layout.padding.padding2,
                vertical = YGTheme.layout.padding.padding1,
            ),
    )
}

@YGPreview
@Composable
private fun YGChipNewPreview() = PreviewBox {
    YGChipNew()
}
