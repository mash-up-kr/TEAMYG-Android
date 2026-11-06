package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toRect
import com.teamyg.parfait.core.designsystem.R as DesignSystemR
import com.teamyg.parfait.core.designsystem.component.ygcamerazoombutton.YGCameraZoomButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.component.ygtext.YGDate
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastHost
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.NamedPreviewParameterProvider
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.model.parfaitToday
import com.teamyg.parfait.feature.camera.impl.component.CameraControlComponent
import com.teamyg.parfait.feature.camera.impl.component.CameraPermissionRequestComponent
import com.teamyg.parfait.feature.camera.impl.model.CameraZoomLevel
import com.teamyg.parfait.feature.camera.impl.model.FlashMode
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraState
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
    if (state.hasPermission) {
        CameraContent(
            toastPolicy = toastPolicy,
            flashMode = state.flashMode,
            zoomLevel = state.zoomLevel,
            isZoomButtonVisible = state.isZoomButtonVisible,
            isCapturing = state.isCapturing,
            onClickShutter = onClickShutter,
            onClickFlip = onClickFlip,
            onClickFlash = onClickFlash,
            onClickZoom = onClickZoom,
            onClickCancel = onClickCancel,
            onViewfinderRectChange = onViewfinderRectChange,
            modifier = modifier,
            cameraFeed = cameraFeed,
        )
    } else {
        CameraPermissionRequestComponent(
            isInitialized = state.isInitialized,
            onClickOpenAppSettings = onClickOpenAppSettings,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )
    }
}

@Composable
private fun CameraContent(
    toastPolicy: YGToastPolicy,
    flashMode: FlashMode,
    zoomLevel: CameraZoomLevel,
    isZoomButtonVisible: Boolean,
    isCapturing: Boolean,
    onClickShutter: () -> Unit,
    onClickFlip: () -> Unit,
    onClickFlash: () -> Unit,
    onClickZoom: () -> Unit,
    onClickCancel: () -> Unit,
    onViewfinderRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier,
    cameraFeed: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        cameraFeed()
        ShutterFlash(
            isCapturing = isCapturing,
            modifier = Modifier.fillMaxSize(),
        )

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
            Spacer(modifier = Modifier.height(10.dp)) // 디자인 토큰에 10dp gap 이 없다
            // 뷰파인더 자리는 위치만 통지한다. 선명 영역 렌더링은 cameraFeed가 맡는다.
            // Box 안에 YGToastHost 를 얹어서, 위치 계산 없이 항상 프레임 윗변에 뜨게 한다
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // 뷰파인더 영역을 그리기에도 쓰므로 debounce 없이 바로 통지한다
                    .onLayoutRectChanged(debounceMillis = 0) { onViewfinderRectChange(it.boundsInRoot.toRect()) },
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

/**
 * 셔터를 누른 순간 화면을 한 번 깜빡여 촬영이 들어갔음을 알린다. 저장이 끝날 때까지 1초 남짓 걸려서다.
 */
@Composable
private fun ShutterFlash(
    isCapturing: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(isCapturing) {
        if (!isCapturing) return@LaunchedEffect
        alpha.snapTo(SHUTTER_FLASH_ALPHA)
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = SHUTTER_FLASH_MILLIS))
    }

    Box(
        modifier = modifier.drawBehind {
            drawRect(color = YGAtomicColors.Gray.Black, alpha = alpha.value)
        },
    )
}

private const val SHUTTER_FLASH_ALPHA = 0.6f
private const val SHUTTER_FLASH_MILLIS = 200

private class CustomCameraScreenPreviewParameterProvider : NamedPreviewParameterProvider<CustomCameraState>() {
    override val entries = listOf(
        "permissionDenied" to CustomCameraState(isInitialized = true, hasPermission = false),
        "permissionGranted" to CustomCameraState(isInitialized = true, hasPermission = true),
        "zoomLevels" to CustomCameraState(
            isInitialized = true,
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
