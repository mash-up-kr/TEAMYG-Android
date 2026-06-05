package com.teamyg.camera.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.camera.impl.component.controls.FlipCameraButton
import com.teamyg.camera.impl.component.controls.ShutterButton
import com.teamyg.camera.impl.component.controls.ZoomLevelRow
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun CameraControlComponent(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ZoomLevelRow(
            zoomRatio = zoomRatio,
            zoomRange = zoomRange,
            onClickZoomLevel = onClickZoomLevel,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TextButton(onClick = onClickCancel) {
                    Text(
                        text = "취소",
                        color = Color.White,
                    )
                }
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                ShutterButton(onClick = onClickShutter)
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                FlipCameraButton(onClick = onClickFlip)
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraControlComponent() =
    PreviewBox {
        CameraControlComponent(
            zoomRatio = 1f,
            zoomRange = 0.5f..10f,
            onClickZoomLevel = {},
            onClickShutter = {},
            onClickFlip = {},
            onClickCancel = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
