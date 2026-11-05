package com.teamyg.parfait.feature.camera.impl.route

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teamyg.parfait.core.designsystem.component.ygtoast.YGToastType
import com.teamyg.parfait.core.designsystem.component.ygtoast.rememberYGToastPolicy
import com.teamyg.parfait.core.designsystem.component.ygtoast.showError
import com.teamyg.parfait.core.designsystem.screen.YGScaffoldV2
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.extension.isGrantedPermission
import com.teamyg.parfait.core.util.jvm.coroutines.runSuspendCatching
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.feature.camera.api.PictureConfirmSource
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.feature.camera.impl.component.CameraFeedLayer
import com.teamyg.parfait.feature.camera.impl.component.CameraPreviewBindingEffect
import com.teamyg.parfait.feature.camera.impl.component.CameraTransitionEffect
import com.teamyg.parfait.feature.camera.impl.component.CameraZoomEffect
import com.teamyg.parfait.feature.camera.impl.component.rememberCameraPreviewState
import com.teamyg.parfait.feature.camera.impl.model.LensFacing
import com.teamyg.parfait.feature.camera.impl.screen.CustomCameraScreen
import com.teamyg.parfait.feature.camera.impl.util.captureViewfinder
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraEffect
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraIntent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraViewModel
import com.teamyg.parfait.feature.groups.canvas.api.NavKeyCanvasMain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun CustomCameraRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    showGuideToast: Boolean = true,
    returnResultOnly: Boolean = false,
    viewModel: CustomCameraViewModel = hiltViewModel(),
) {
    val activity: Activity? = LocalActivity.current
    val context: Context = activity ?: LocalContext.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var viewfinderRect by remember { mutableStateOf<Rect?>(null) }
    var feedRect by remember { mutableStateOf<Rect?>(null) }
    val captureScope = rememberCoroutineScope()
    val cameraPreviewState = rememberCameraPreviewState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.processIntent(
            CustomCameraIntent.OnPermissionRequestResult(granted = granted),
        )
    }

    val toastPolicy = rememberYGToastPolicy()
    val guideToastMessage = stringResource(R.string.camera_custom_guide_toast)
    val captureFailedMessage = stringResource(R.string.camera_capture_failed)
    var hasShownGuideToast by rememberSaveable { mutableStateOf(false) }

    // 촬영 요령을 말하는 토스트라, 권한이 없어 찍을 수 없는 화면에서는 할 말이 아니다.
    // 설정에서 권한을 켜고 돌아오면 그때 처음 뜬다
    SideEffect(state.hasPermission) {
        if (showGuideToast && state.hasPermission && !hasShownGuideToast) {
            toastPolicy.show(YGToastType.Edit(guideToastMessage))
            hasShownGuideToast = true
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CustomCameraEffect.RequestPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                is CustomCameraEffect.OpenAppSettings -> {
                    context.startActivity(context.buildAppSettingsIntent())
                }

                is CustomCameraEffect.CaptureImage -> {
                    val capture = imageCapture ?: run {
                        viewModel.processIntent(CustomCameraIntent.OnCaptureFailed)
                        return@collect
                    }
                    val captureJob = captureScope.launch {
                        val saved = runSuspendCatching {
                            capture.captureViewfinder(
                                file = effect.file,
                                viewfinderRect = viewfinderRect,
                                feedRect = feedRect,
                                isFrontFacing = state.lensFacing == LensFacing.FRONT,
                                isViewPortApplied = cameraPreviewState.isViewPortApplied,
                            )
                        }.isSuccess

                        viewModel.processIntent(
                            if (saved) {
                                CustomCameraIntent.OnCaptureSaved(effect.file)
                            } else {
                                CustomCameraIntent.OnCaptureFailed
                            },
                        )
                    }
                    captureJob.invokeOnCompletion { cause ->
                        // Route 만 사라지고 VM 은 남는 경우(재생성 등) 셔터가 잠긴 채 남지 않게 한다.
                        // 본문 시작 전에 취소된 잡도 여기서 잡힌다
                        if (cause is CancellationException) {
                            viewModel.processIntent(CustomCameraIntent.OnCaptureCancelled)
                        }
                    }
                }

                is CustomCameraEffect.Cancel -> navigator.popUpTo<NavKeyCanvasMain>()

                is CustomCameraEffect.CaptureFailed -> {
                    toastPolicy.showError(captureFailedMessage)
                }

                is CustomCameraEffect.NavigateToConfirm -> {
                    navigator.goTo(
                        NavKeyPictureConfirm(
                            uri = effect.uri,
                            source = PictureConfirmSource.CAMERA,
                            returnResultOnly = returnResultOnly,
                        ),
                    )
                }
            }
        }
    }

    // 배경 편집에서 온 경로만 가로채 캔버스로 되감는다. 캔버스에서 바로 온 경로는 기본
    // 뒤로가기가 이미 캔버스라 두면 예측형 뒤로가기 전환이 산다
    BackHandler(enabled = returnResultOnly) {
        viewModel.processIntent(CustomCameraIntent.OnCancel)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val granted = context.isGrantedPermission(permission = Manifest.permission.CAMERA)

        viewModel.processIntent(CustomCameraIntent.OnPermissionChecked(granted))
    }

    SideEffect(state.flashMode, imageCapture) {
        imageCapture?.flashMode = state.flashMode.imageCaptureFlashMode
    }

    CameraPreviewBindingEffect(
        state = cameraPreviewState,
        lensFacing = state.lensFacing,
        shouldUseUltraWideCamera = state.shouldUseUltraWideCamera,
        hasPermission = state.hasPermission,
        onImageCaptureReady = { imageCapture = it },
        onZoomRangeReady = { range, hasUltraWideCamera ->
            viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(range, hasUltraWideCamera))
        },
    )
    CameraZoomEffect(
        state = cameraPreviewState,
        zoomRatio = state.cameraZoomRatio,
    )
    CameraTransitionEffect(state = cameraPreviewState)

    // 카메라 피드는 시스템 바 아래까지 덮어야 하므로 innerPadding을 화면에 먹이지 않는다.
    // 인셋은 CustomCameraScreen의 컨트롤 영역이 직접 처리한다.
    YGScaffoldV2 { _ ->
        CustomCameraScreen(
            state = state,
            toastPolicy = toastPolicy,
            onClickOpenAppSettings = { viewModel.processIntent(CustomCameraIntent.OnClickOpenAppSettings) },
            onClickShutter = { viewModel.processIntent(CustomCameraIntent.OnClickShutter) },
            onClickFlip = { viewModel.processIntent(CustomCameraIntent.OnClickFlip) },
            onClickCancel = { viewModel.processIntent(CustomCameraIntent.OnCancel) },
            onClickFlash = { viewModel.processIntent(CustomCameraIntent.OnClickFlash) },
            onClickZoom = { viewModel.processIntent(CustomCameraIntent.OnClickZoom) },
            modifier = modifier,
            onViewfinderRectChange = { viewfinderRect = it },
            cameraFeed = {
                CameraFeedLayer(
                    viewfinderRect = { viewfinderRect },
                    transition = cameraPreviewState.transition,
                    onFeedRectChange = { feedRect = it },
                    onTapViewfinder = cameraPreviewState::focusAt,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AndroidView(
                        factory = { cameraPreviewState.previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
    }
}
