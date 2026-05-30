package com.teamyg.camera.impl.route

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.camera.impl.screen.CustomCameraScreen
import com.teamyg.camera.impl.util.CameraFileProvider
import com.teamyg.camera.impl.vm.CustomCameraEffect
import com.teamyg.camera.impl.vm.CustomCameraIntent
import com.teamyg.camera.impl.vm.CustomCameraViewModel
import com.teamyg.navigation.Navigator

@Composable
internal fun CustomCameraRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: CustomCameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted))
        if (!granted) {
            navigator.onBack()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                CustomCameraEffect.RequestPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                CustomCameraEffect.CaptureImage -> {
                    val capture = imageCapture ?: run {
                        viewModel.processIntent(CustomCameraIntent.OnCaptureFailed)
                        return@collect
                    }
                    val file = CameraFileProvider.createImageFile(context)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val contentUri = CameraFileProvider.toContentUri(context, file)
                                viewModel.processIntent(
                                    CustomCameraIntent.OnCaptureSaved(contentUri.toString()),
                                )
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

                CustomCameraEffect.NavigateToBack -> navigator.onBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.processIntent(CustomCameraIntent.OnPermissionResult(granted))
    }

    CustomCameraScreen(
        state = state,
        onClickGrantPermission = { viewModel.processIntent(CustomCameraIntent.OnRequestPermission) },
        onImageCaptureReady = { imageCapture = it },
        onZoomRangeReady = { viewModel.processIntent(CustomCameraIntent.OnZoomRangeReady(it)) },
        onClickZoomLevel = { viewModel.processIntent(CustomCameraIntent.OnClickZoomLevel(it)) },
        onClickShutter = { viewModel.processIntent(CustomCameraIntent.OnClickShutter) },
        onClickFlip = { viewModel.processIntent(CustomCameraIntent.OnClickFlip) },
        onClickCancel = { viewModel.processIntent(CustomCameraIntent.OnCancel) },
        modifier = modifier,
    )
}
