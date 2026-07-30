package com.teamyg.parfait.core.designsystem.component.ygcolorchip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
fun YGChipColorIndicator(modifier: Modifier = Modifier, isChecked: Boolean) {
    val color = if(isChecked) YGAtomicColors.Cherry.Cherry else Color.Transparent
    Box(
        modifier = modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@YGPreview
@Composable
private fun YGChipColorIndicatorPreview() = PreviewBox {
    Column() {
        YGChipColorIndicator(isChecked = true)
        YGChipColorIndicator(isChecked = false)
    }

}
