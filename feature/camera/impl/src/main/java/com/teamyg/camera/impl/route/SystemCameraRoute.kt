package com.teamyg.camera.impl.route

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.LocalResultEventBus
import com.teamyg.camera.impl.screen.SystemCameraScreen
import com.teamyg.camera.impl.util.CameraFileProvider
import com.teamyg.camera.impl.vm.SystemCameraEffect
import com.teamyg.camera.impl.vm.SystemCameraIntent
import com.teamyg.camera.impl.vm.SystemCameraViewModel
import com.teamyg.navigation.Navigator

@Composable
internal fun SystemCameraRoute(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: SystemCameraViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
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
        viewModel.processIntent(SystemCameraIntent.OnPermissionResult(granted))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                SystemCameraEffect.RequestPermission -> {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }

                SystemCameraEffect.LaunchCamera -> {
                    val uri = CameraFileProvider.createImageUri(context)
                    pendingUri = uri
                    takePictureLauncher.launch(uri)
                    viewModel.processIntent(SystemCameraIntent.OnCaptureLaunched)
                }

                is SystemCameraEffect.ReturnResult -> {
                    resultEventBus.sendResult(effect.uri)
                    navigator.onBack()
                }

                SystemCameraEffect.Back -> navigator.onBack()
            }
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.processIntent(SystemCameraIntent.OnPermissionResult(granted))
    }

    SystemCameraScreen(
        state = state,
        onClickGrantPermission = { viewModel.processIntent(SystemCameraIntent.OnRequestPermission) },
        onClickRetry = { viewModel.processIntent(SystemCameraIntent.OnRetry) },
        onClickCancel = { viewModel.processIntent(SystemCameraIntent.OnCancel) },
        modifier = modifier,
    )
}
