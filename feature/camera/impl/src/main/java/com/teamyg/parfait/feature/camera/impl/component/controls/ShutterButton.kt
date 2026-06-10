package com.teamyg.parfait.feature.camera.impl.component.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.ui.preview.PreviewBox
import com.teamyg.parfait.core.ui.preview.YGPreview

@Composable
internal fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(
                width = 4.dp,
                color = Color.Gray,
                shape = CircleShape,
            ),
    ) {}
}

@YGPreview
@Composable
private fun PreviewShutterButton() = PreviewBox {
    ShutterButton(
        onClick = {},
    )
}
