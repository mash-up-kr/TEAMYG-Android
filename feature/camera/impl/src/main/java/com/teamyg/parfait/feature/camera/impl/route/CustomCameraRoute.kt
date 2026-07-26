package com.teamyg.parfait.feature.camera.impl.route

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.feature.camera.impl.component.CameraFeedLayer
import com.teamyg.parfait.feature.camera.impl.component.CameraPreviewViewComponent
import com.teamyg.parfait.feature.camera.impl.screen.CustomCameraScreen
import com.teamyg.parfait.feature.camera.impl.util.saveViewfinderCapture
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraEffect
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraIntent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.extension.isGrantedPermission
import com.teamyg.parfait.core.util.android.extension.shouldShowRationale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CustomCameraRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CustomCameraViewModel = hiltViewModel(),
) {
    val activity: Activity? = LocalActivity.current
    val context: Context = activity ?: LocalContext.current

    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var viewfinderRect by remember { mutableStateOf<Rect?>(null) }
    var feedRect by remember { mutableStateOf<Rect?>(null) }
    val captureScope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.processIntent(
            CustomCameraIntent.OnPermissionRequestResult(
                granted = granted,
                shouldShowRationale = activity?.shouldShowRationale(Manifest.permission.CAMERA) == true,
            ),
        )
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
                    val file = effect.file
                    val isFrontFacing = state.lensFacing == CameraSelector.LENS_FACING_FRONT

                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val rotation = image.imageInfo.rotationDegrees
                                val captured = image.use { it.toBitmap() }

                                captureScope.launch {
                                    val saved = runCatching {
                                        withContext(Dispatchers.IO) {
                                            saveViewfinderCapture(
                                                captured = captured,
                                                rotationDegrees = rotation,
                                                viewfinderRect = viewfinderRect,
                                                feedRect = feedRect,
                                                isFrontFacing = isFrontFacing,
                                                file = file,
                                            )
                                        }
                                    }.isSuccess

                                    viewModel.processIntent(
                                        if (saved) {
                                            CustomCameraIntent.OnCaptureSaved(file)
                                        } else {
                                            CustomCameraIntent.OnCaptureFailed
                                        },
                                    )
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                viewModel.processIntent(CustomCameraIntent.OnCaptureFailed)
                            }
                        },
                    )
                }

                is CustomCameraEffect.ReturnResult -> {
                    resultEventBus.sendResult(effect.uri)
                    navigator.onBack()
                }

                is CustomCameraEffect.NavigateToConfirm -> {
                    navigator.goTo(NavKeyPictureConfirm(uri = effect.uri))
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        val granted = context.isGrantedPermission(permission = Manifest.permission.CAMERA)

        viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted))
        onPauseOrDispose { }
    }

    val cameraPreviewHandle = CameraPreviewViewComponent(
        lensFacing = state.lensFacing,
        zoomRatio = state.zoomRatio,
        onImageCaptureReady = { imageCapture = it },
        onZoomRangeReady = { viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(it)) },
    )

    CustomCameraScreen(
        state = state,
        onClickGrantPermission = { viewModel.processIntent(CustomCameraIntent.OnRequestPermission) },
        onClickOpenAppSettings = { viewModel.processIntent(CustomCameraIntent.OnOpenAppSettings) },
        onClickZoomLevel = { viewModel.processIntent(CustomCameraIntent.OnClickZoomLevel(it)) },
        onClickShutter = { viewModel.processIntent(CustomCameraIntent.OnClickShutter) },
        onClickFlip = { viewModel.processIntent(CustomCameraIntent.OnClickFlip) },
        onClickCancel = { viewModel.processIntent(CustomCameraIntent.OnCancel) },
        onClickFlash = {},
        modifier = modifier,
        onViewfinderRectChange = { viewfinderRect = it },
        cameraFeed = {
            CameraFeedLayer(
                previewView = cameraPreviewHandle.previewView,
                camera = cameraPreviewHandle.camera.value,
                viewfinderRect = { viewfinderRect },
                onFeedRectChange = { feedRect = it },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
