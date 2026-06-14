package com.teamyg.parfait.feature.camera.impl.route

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.teamyg.parfait.feature.camera.impl.screen.SystemCameraScreen
import com.teamyg.parfait.feature.camera.impl.util.CameraFileProvider
import com.teamyg.parfait.feature.camera.impl.util.CameraPermissionUtil
import com.teamyg.parfait.feature.camera.impl.viewmodel.SystemCameraEffect
import com.teamyg.parfait.feature.camera.impl.viewmodel.SystemCameraIntent
import com.teamyg.parfait.feature.camera.impl.viewmodel.SystemCameraState
import com.teamyg.parfait.feature.camera.impl.viewmodel.SystemCameraViewModel
import com.teamyg.parfait.core.navigation.Navigator

@Composable
internal fun SystemCameraRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SystemCameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val resultEventBus = LocalResultEventBus.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingUri
        pendingUri = null

        viewModel.processIntent(
            SystemCameraIntent.OnCaptureResult(
                success = success,
                uri = uri?.toString(),
            ),
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.processIntent(
            SystemCameraIntent.OnPermissionRequestResult(
                granted = granted,
                shouldShowRationale = CameraPermissionUtil.shouldShowRationale(
                    activity = activity,
                    permission = Manifest.permission.CAMERA,
                ),
            ),
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SystemCameraEffect.RequestPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                is SystemCameraEffect.OpenAppSettings -> {
                    CameraPermissionUtil.openAppSettings(context)
                }

                is SystemCameraEffect.LaunchCamera -> {
                    val uri = CameraFileProvider.createImageUri(context)
                    pendingUri = uri
                    takePictureLauncher.launch(uri)
                    viewModel.processIntent(SystemCameraIntent.OnCaptureLaunched)
                }

                is SystemCameraEffect.ReturnResult -> {
                    resultEventBus.sendResult(effect.uri)
                    navigator.onBack()
                }
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        when (state) {
            is SystemCameraState.Init,
            is SystemCameraState.RequestingPermission,
            -> viewModel.processIntent(SystemCameraIntent.OnPermissionResult(granted))

            else -> Unit
        }

        onPauseOrDispose {}
    }

    SystemCameraScreen(
        state = state,
        onClickGrantPermission = { viewModel.processIntent(SystemCameraIntent.OnRequestPermission) },
        onClickOpenAppSettings = { viewModel.processIntent(SystemCameraIntent.OnOpenAppSettings) },
        onClickRetry = { viewModel.processIntent(SystemCameraIntent.OnRetry) },
        onClickCancel = { viewModel.processIntent(SystemCameraIntent.OnCancel) },
        modifier = modifier,
    )
}
