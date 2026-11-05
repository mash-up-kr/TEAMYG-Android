package com.teamyg.parfait.feature.camera.impl.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygcamerashutter.YGCameraShutter
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.model.FlashMode

@Composable
internal fun CameraControlComponent(
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    flashMode: FlashMode,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Absolute.SpaceBetween,
    ) {
        YGCircleButton(
            iconResource = if (flashMode == FlashMode.ON) {
                R.drawable.ic_lightning_fill
            } else {
                R.drawable.ic_lightning
            },
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = onClickFlash,
        )

        YGCameraShutter(onClick = onClickShutter)
        YGCircleButton(
            iconResource = R.drawable.ic_reverse,
            type = YGCircleButtonType.Default,
            contentDescription = null,
            onClick = onClickFlip,
        )
    }
}

@YGPreview
@Composable
private fun PreviewCameraControlComponent() = PreviewBox {
    CameraControlComponent(
        onClickShutter = {},
        onClickFlip = {},
        onClickFlash = {},
        flashMode = FlashMode.OFF,
        modifier = Modifier.fillMaxWidth(),
    )
}
