package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygcamerazoombutton.YGCameraZoomButton
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.camera.impl.component.CameraControlComponent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraState
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.component.CameraPermissionRequestComponent
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.viewmodel.FlashMode
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.model.parfaitToday
import kotlinx.datetime.format

@Composable
internal fun CustomCameraScreen(
    state: CustomCameraState,
    toastPolicy: YGToastPolicy,
    onClickOpenAppSettings: () -> Unit,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickZoom: () -> Unit,
    onClickCancel: () -> Unit,
    onViewfinderRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    cameraFeed: @Composable () -> Unit,
) {
    when (state.hasPermission) {
        true -> CameraContent(
            toastPolicy = toastPolicy,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickFlash = onClickFlash,
            onClickZoom = onClickZoom,
            onClickCancel = onClickCancel,
            onViewfinderRectChange = onViewfinderRectChange,
            modifier = modifier,
            cameraFeed = cameraFeed,
            flashMode = state.flashMode,
            zoomLevel = state.zoomLevel,
            isZoomButtonVisible = state.isZoomButtonVisible,
        )

        false -> CameraPermissionRequestComponent(
            isInit = state.isInit,
            onClickOpenAppSettings = onClickOpenAppSettings,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    toastPolicy: YGToastPolicy,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickZoom: () -> Unit,
    onClickCancel: () -> Unit,
    onViewfinderRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    cameraFeed: @Composable () -> Unit,
    flashMode: FlashMode,
    zoomLevel: CameraZoomLevel,
    isZoomButtonVisible: Boolean,
) {
    Box(modifier = modifier.fillMaxSize()) {
        cameraFeed()

        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                    bottom = YGTheme.layout.padding.padding1,
                ),
        ) {
            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap5))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val today = remember { parfaitToday() }
                YGDate(
                    date = today.format(DateTextFormat.monthDayFormat),
                    day = today.format(DateTextFormat.weekdayFormat),
                )
                YGCircleButton(
                    iconResource = DesignSystemR.drawable.ic_close,
                    type = YGCircleButtonType.Secondary,
                    contentDescription = null,
                    onClick = onClickCancel,
                )
            }
            Spacer(modifier = Modifier.height(10.dp)) // 10.dp가 없어서 넣었습니다
            // 뷰파인더 자리는 위치만 통지한다. 선명 영역 렌더링은 cameraFeed가 맡는다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        onViewfinderRectChange(coordinates.boundsInRoot())
                    },
            ) {
                YGToastHost(
                    policy = toastPolicy,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
                if (isZoomButtonVisible) {
                    YGCameraZoomButton(
                        label = zoomLevel.label,
                        onClick = onClickZoom,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = YGTheme.layout.padding.padding4),
                    )
                }
            }

            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

            CameraControlComponent(
                onClickShutter = onClickShutter,
                onClickFlip = onClickFlip,
                onClickFlash = onClickFlash,
                flashMode = flashMode,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private class CustomCameraScreenPreviewParameterProvider : NamedPreviewParameterProvider<CustomCameraState>() {
    override val entries = listOf(
        "permissionDenied" to CustomCameraState(isInit = true, hasPermission = false),
        "permissionGranted" to CustomCameraState(isInit = true, hasPermission = true),
        "zoomLevels" to CustomCameraState(
            isInit = true,
            hasPermission = true,
            zoomLevels = listOf(CameraZoomLevel.ONE, CameraZoomLevel.TWO, CameraZoomLevel.HALF),
        ),
    )
}

@YGPreview
@Composable
private fun PreviewCustomCameraScreen(
    @PreviewParameter(CustomCameraScreenPreviewParameterProvider::class) state: CustomCameraState,
) = PreviewBox {
    CustomCameraScreen(
        state = state,
        toastPolicy = rememberYGToastPolicy(),
        onClickOpenAppSettings = {},
        onClickShutter = {},
        onClickFlip = {},
        onClickFlash = {},
        onClickZoom = {},
        onClickCancel = {},
        onViewfinderRectChange = {},
        modifier = Modifier.fillMaxSize(),
        cameraFeed = @Composable {},
    )
}
