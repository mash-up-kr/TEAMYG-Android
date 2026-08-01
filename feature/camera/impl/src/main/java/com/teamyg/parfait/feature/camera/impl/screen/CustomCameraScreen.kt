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
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import com.teamyg.parfait.core.designsystem.component.ygiconbutton.YGIconButtonSize
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.feature.camera.impl.component.CameraControlComponent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraState
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.feature.camera.impl.component.CameraPermissionRequestComponent
import com.teamyg.parfait.feature.camera.impl.component.YGRoundIconButton
import com.teamyg.parfait.feature.camera.impl.viewmodel.FlashMode
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import kotlinx.datetime.format
import kotlin.time.Clock

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
    toastPolicy: YGToastPolicy,
    onViewfinderRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    cameraFeed: @Composable () -> Unit,
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
            toastPolicy = toastPolicy,
            onViewfinderRectChange = onViewfinderRectChange,
            modifier = modifier,
            cameraFeed = cameraFeed,
            flashMode = state.flashMode,
        )

        false -> CameraPermissionRequestComponent(
            isInit = state.isInit,
            permanentlyDenied = state.permanentlyDenied,
            onClickGrantPermission = onClickGrantPermission,
            onClickOpenAppSettings = onClickOpenAppSettings,
            onClickCancel = onClickCancel,
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
    onClickFlash: () -> Unit,
    onClickCancel: () -> Unit,
    onViewfinderRectChange: (Rect) -> Unit,
    toastPolicy: YGToastPolicy,
    modifier: Modifier = Modifier,
    cameraFeed: @Composable () -> Unit,
    flashMode: FlashMode,
) {
    Box(modifier = modifier.fillMaxSize()) {
        cameraFeed()

        Column(
            modifier = Modifier
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
                val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
                YGDate(
                    date = today.format(DateTextFormat.monthDayFormat),
                    day = today.format(DateTextFormat.weekdayFormat),
                )
                YGRoundIconButton(
                    iconResource = DesignSystemR.drawable.ic_close,
                    size = YGIconButtonSize.SIZE_44,
                    contentDescription = null,
                    onClick = onClickCancel,
                )
            }
            Spacer(modifier = Modifier.height(YGTheme.layout.padding.padding3))
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
                        .requiredWidth(LocalConfiguration.current.screenWidthDp.dp)
                        .windowInsetsPadding(WindowInsets.systemBars),
                )
            }

            Spacer(modifier = Modifier.height(YGTheme.layout.gap.gap3))

            CameraControlComponent(
                zoomRatio = zoomRatio,
                zoomRange = zoomRange,
                onClickZoomLevel = onClickZoomLevel,
                onClickShutter = onClickShutter,
                onClickFlip = onClickFlip,
                onClickFlash = onClickFlash,
                onClickCancel = onClickCancel,
                flashMode = flashMode,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

//@YGPreview
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
        onViewfinderRectChange = {},
        toastPolicy = rememberYGToastPolicy(),
        modifier = Modifier.fillMaxSize(),
        cameraFeed = @Composable {},
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
        onViewfinderRectChange = {},
        toastPolicy = rememberYGToastPolicy(),
        modifier = Modifier.fillMaxSize(),
        cameraFeed = @Composable {},
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
        onViewfinderRectChange = {},
        toastPolicy = rememberYGToastPolicy(),
        modifier = Modifier.fillMaxSize(),
        cameraFeed = @Composable {},
    )
}
