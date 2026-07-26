package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.teamyg.parfait.core.designsystem.R
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButton
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.camera.impl.component.CameraControlComponent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraState
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun CustomCameraScreen(
    state: CustomCameraState,
    onClickGrantPermission: () -> Unit,
    onClickOpenAppSettings: () -> Unit,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cameraBackground: @Composable () -> Unit,
    cameraViewfinder: @Composable () -> Unit,
) {
    when (state.hasPermission) {
        true -> CameraContent(
            zoomRatio = state.zoomRatio,
            zoomRange = state.zoomRange,
            onClickZoomLevel = onClickZoomLevel,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickFlash = onClickFlash,
            onClickCancel = onClickCancel,
            modifier = modifier,
            cameraBackground = cameraBackground,
            cameraViewfinder = cameraViewfinder,
        )

        false -> CameraContent(
            zoomRatio = state.zoomRatio,
            zoomRange = state.zoomRange,
            onClickZoomLevel = onClickZoomLevel,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickFlash = onClickFlash,
            onClickCancel = onClickCancel,
            modifier = modifier,
            cameraBackground = cameraBackground,
            cameraViewfinder = cameraViewfinder,
        )
//        false -> CameraPermissionRequestComponent(
//            isInit = state.isInit,
//            permanentlyDenied = state.permanentlyDenied,
//            onClickGrantPermission = onClickGrantPermission,
//            onClickOpenAppSettings = onClickOpenAppSettings,
//            onClickCancel = onClickCancel,
//            modifier = modifier,
//        )
    }
}

@Composable
private fun CameraContent(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cameraBackground: @Composable () -> Unit,
    cameraViewfinder: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        cameraBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = YGTheme.layout.padding.padding7),
        ) {
            Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding6))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                YGDate("dad", "asd")
                YGIconButton(
                    iconResource = R.drawable.ic_close,
                    size = YGIconButtonSize.SIZE_44,
                    contentDescription = null,
                    onClick = onClickCancel,
                    isEnabled = true,
                )
            }
            Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding3))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                cameraViewfinder()
            }
            Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding4))

            CameraControlComponent(
                zoomRatio = zoomRatio,
                zoomRange = zoomRange,
                onClickZoomLevel = onClickZoomLevel,
                onClickShutter = onClickShutter,
                onClickFlip = onClickFlip,
                onClickFlash = onClickFlash,
                onClickCancel = onClickCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewCustomCameraScreenPermissionDenied() = PreviewBox {
    CustomCameraScreen(
        state = CustomCameraState(
            isInit = true,
            hasPermission = false,
        ),
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickZoomLevel = {},
        onClickShutter = {},
        onClickFlip = {},
        onClickFlash = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
        cameraBackground = @Composable {},
        cameraViewfinder = @Composable {},
    )
}

@YGPreview
@Composable
private fun PreviewCustomCameraScreenPermissionPermanentlyDenied() = PreviewBox {
    CustomCameraScreen(
        state = CustomCameraState(
            isInit = true,
            hasPermission = false,
            permanentlyDenied = true,
        ),
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickZoomLevel = {},
        onClickShutter = {},
        onClickFlip = {},
        onClickCancel = {},
        onClickFlash = {},
        modifier = Modifier.fillMaxSize(),
        cameraBackground = @Composable {},
        cameraViewfinder = @Composable {},
    )
}

@YGPreview
@Composable
private fun PreviewCustomCameraScreenPermissionGranted() = PreviewBox {
    CustomCameraScreen(
        state = CustomCameraState(
            isInit = true,
            hasPermission = true,
            permanentlyDenied = false,
        ),
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickZoomLevel = {},
        onClickShutter = {},
        onClickFlip = {},
        onClickCancel = {},
        onClickFlash = {},
        modifier = Modifier.fillMaxSize(),
        cameraBackground = @Composable {},
        cameraViewfinder = @Composable {},
    )
}
