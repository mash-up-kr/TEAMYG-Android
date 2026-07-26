package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.camera.impl.component.controls.ShutterButton
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun CameraControlComponent(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
//        ZoomLevelRow(
//            zoomRatio = zoomRatio,
//            zoomRange = zoomRange,
//            onClickZoomLevel = onClickZoomLevel,
//        )

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
                YGIconButton(
                    iconResource = R.drawable.ic_reverse,
                    size = YGIconButtonSize.SIZE_44,
                    contentDescription = null,
                    onClick = onClickFlip,
                    isEnabled = true,
                )
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
                YGIconButton(
                    iconResource = R.drawable.ic_lightning,
                    size = YGIconButtonSize.SIZE_44,
                    contentDescription = null,
                    onClick = onClickFlash,
                    isEnabled = true,
                )
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraControlComponent() = PreviewBox {
    CameraControlComponent(
        zoomRatio = 1f,
        zoomRange = 0.5f..10f,
        onClickZoomLevel = {},
        onClickShutter = {},
        onClickFlip = {},
        onClickFlash = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
