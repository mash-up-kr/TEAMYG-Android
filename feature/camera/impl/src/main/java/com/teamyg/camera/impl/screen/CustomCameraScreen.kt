package com.teamyg.camera.impl.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.teamyg.camera.impl.component.CameraControlComponent
import com.teamyg.camera.impl.component.CameraPermissionRequestComponent
import com.teamyg.camera.impl.component.CameraZoomIndicatorComponent
import com.teamyg.camera.impl.viewmodel.CustomCameraState
import com.teamyg.core.ui.preview.PreviewBox
import com.teamyg.core.ui.preview.YGPreview

@Composable
internal fun CustomCameraScreen(
    state: CustomCameraState,
    onClickGrantPermission: () -> Unit,
    onClickOpenAppSettings: () -> Unit,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit,
) {
    when (state.hasPermission) {
        true -> CameraContent(
            zoomRatio = state.zoomRatio,
            zoomRange = state.zoomRange,
            onClickZoomLevel = onClickZoomLevel,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickCancel = onClickCancel,
            modifier = modifier,
            cameraPreview = cameraPreview,
        )

        false -> CameraPermissionRequestComponent(
            isInit = state.isInit,
            permanentlyDenied = state.permanentlyDenied,
            onClickGrantPermission = onClickGrantPermission,
            onClickOpenAppSettings = onClickOpenAppSettings,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    zoomRatio: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onClickZoomLevel: (Float) -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cameraPreview: @Composable () -> Unit,
) {
    Column(modifier = modifier.background(Color.Black)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            cameraPreview()

            CameraZoomIndicatorComponent(
                zoomRatio = zoomRatio,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        CameraControlComponent(
            zoomRatio = zoomRatio,
            zoomRange = zoomRange,
            onClickZoomLevel = onClickZoomLevel,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickCancel = onClickCancel,
            modifier = Modifier.fillMaxWidth(),
        )
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
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
        cameraPreview = @Composable {},
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
        modifier = Modifier.fillMaxSize(),
        cameraPreview = @Composable {},
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
        modifier = Modifier.fillMaxSize(),
        cameraPreview = @Composable {},
    )
}
