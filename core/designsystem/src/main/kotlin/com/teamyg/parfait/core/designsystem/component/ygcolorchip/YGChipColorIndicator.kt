package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors

@Composable
fun YGChipColorIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(YGAtomicColors.Cherry.Cherry),
    )
}

@Preview
@Composable
private fun YGChipColorIndicatorPreview() {
    YGChipColorIndicator()
}
