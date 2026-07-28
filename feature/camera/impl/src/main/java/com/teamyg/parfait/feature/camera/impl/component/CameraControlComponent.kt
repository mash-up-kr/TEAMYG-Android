package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.feature.camera.impl.component.controls.ShutterButton
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.viewmodel.FlashMode

@Composable
internal fun CameraControlComponent(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickCancel: () -> Unit,
    flashMode: FlashMode,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
        ) {
            YGRoundIconButton(
                iconResource = if (flashMode ==
                    FlashMode.ON
                ) {
                    R.drawable.ic_lightning_active
                } else {
                    R.drawable.ic_lightning
                },
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClickFlash,
            )

            ShutterButton(onClick = onClickShutter)
            YGRoundIconButton(
                iconResource = R.drawable.ic_reverse,
                size = YGIconButtonSize.SIZE_44,
                contentDescription = null,
                onClick = onClickFlip,
            )
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
        flashMode = FlashMode.OFF,
        modifier = Modifier.fillMaxWidth(),
    )
}
