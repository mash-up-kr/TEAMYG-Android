package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

private val AccentZoomColor = Color(0xFFFFC107)

@Composable
internal fun CameraZoomIndicatorComponent(
    zoomRatio: Float,
    modifier: Modifier = Modifier,
) {
    val localLocale = LocalLocale.current.platformLocale

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp,
            ),
    ) {
        Text(
            text = String.format(
                localLocale,
                "%.1f×",
                zoomRatio,
            ),
            color = AccentZoomColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@YGPreview
@Composable
private fun PreviewCameraZoomIndicatorComponent() = PreviewBox {
    CameraZoomIndicatorComponent(zoomRatio = 1f)
}
