package com.teamyg.parfait.feature.camera.impl.route

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.parfait.feature.camera.impl.component.CameraBackgroundPreview
import com.teamyg.parfait.feature.camera.impl.component.CameraViewfinderPreview
import com.teamyg.parfait.feature.camera.impl.component.CameraPreviewViewComponent
import com.teamyg.parfait.feature.camera.impl.screen.CustomCameraScreen
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraEffect
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraIntent
import com.teamyg.parfait.feature.camera.impl.viewmodel.CustomCameraViewModel
import com.teamyg.parfait.core.navigation.Navigator
import com.teamyg.parfait.feature.camera.api.NavKeyPictureConfirm
import com.teamyg.parfait.core.util.android.extension.buildAppSettingsIntent
import com.teamyg.parfait.core.util.android.extension.isGrantedPermission
import com.teamyg.parfait.core.util.android.extension.shouldShowRationale

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
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                viewModel.processIntent(CustomCameraIntent.OnCaptureSaved(file))
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

    val cameraPreviewViews = CameraPreviewViewComponent(
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
        cameraBackground = {
            CameraBackgroundPreview(
                previewView = cameraPreviewViews.backgroundPreviewView,
                modifier = Modifier.fillMaxSize(),
            )
        },
        cameraViewfinder = {
            CameraViewfinderPreview(
                previewView = cameraPreviewViews.foregroundPreviewView,
                camera = cameraPreviewViews.camera.value,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
