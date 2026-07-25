package com.teamyg.parfait.feature.camera.impl.component.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(width = 1.dp, color = YGAtomicColors.Transparency.Black5, shape = CircleShape)
            .clickable{ onClick() },
        contentAlignment = Alignment.Center,
    ){
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(color = YGAtomicColors.Gray.Gray900),
        )
    }
}

@YGPreview
@Composable
private fun PreviewShutterButton() = PreviewBox {
    ShutterButton(
        onClick = {},
    )
}
