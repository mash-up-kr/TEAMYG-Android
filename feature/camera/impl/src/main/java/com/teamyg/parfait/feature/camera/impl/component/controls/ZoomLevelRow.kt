package com.teamyg.parfait.feature.camera.impl.component.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import kotlin.math.abs

private val AccentZoomColor = Color(0xFFFFC107)
private val DefaultZoomLevels = listOf(0.5f, 1f, 2f, 4f, 10f)
private val ChipBackgroundColor = Color(0xFF1F1F1F)
private const val ZOOM_EPSILON = 0.05f

@Composable
internal fun ZoomLevelRow(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableLevels = DefaultZoomLevels.filter { it in zoomRange }

    if (availableLevels.isNotEmpty()) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            availableLevels.forEach { level ->
                ZoomLevelChip(
                    level = level,
                    isSelected = isSameZoom(zoomRatio, level),
                    onClick = { onClickZoomLevel(level) },
                )
            }
        }
    }
}

@Composable
private fun ZoomLevelChip(
    level: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color = if (isSelected) ChipBackgroundColor else Color.Transparent)
            .clickableYGNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatZoomLabel(level),
            color = if (isSelected) AccentZoomColor else Color.White,
            fontSize = if (isSelected) 14.sp else 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

private fun formatZoomLabel(level: Float): String = String.format(
    if (level >= 1f) "%d" else "%s",
    if (level >= 1f) level.toInt() else level.toString().removePrefix("0"),
)

private fun isSameZoom(
    a: Float,
    b: Float,
): Boolean = abs(a - b) < ZOOM_EPSILON
