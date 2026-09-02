package com.teamyg.parfait.feature.camera.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.feature.camera.impl.R
import com.teamyg.parfait.feature.camera.impl.viewmodel.SystemCameraState
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview

@Composable
internal fun SystemCameraScreen(
    state: SystemCameraState,
    onClickGrantPermission: () -> Unit,
    onClickOpenAppSettings: () -> Unit,
    onClickRetry: () -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            SystemCameraState.Init,
            SystemCameraState.Launching,
            -> {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.camera_opening))
            }

            is SystemCameraState.RequestingPermission -> {
                when (state.permanentlyDenied) {
                    true -> {
                        Text(text = stringResource(R.string.camera_permission_permanently_denied_message))
                        Button(onClick = onClickOpenAppSettings) {
                            Text(text = stringResource(R.string.camera_open_settings))
                        }
                    }

                    false -> {
                        Text(text = stringResource(R.string.camera_permission_required_message))
                        Button(onClick = onClickGrantPermission) {
                            Text(text = stringResource(R.string.camera_request_permission))
                        }
                    }
                }
            }

            SystemCameraState.Capturing -> {
                CircularProgressIndicator()
                Text(text = stringResource(R.string.camera_capturing))
            }

            SystemCameraState.Failed -> {
                Text(text = stringResource(R.string.camera_capture_failed))
                Button(onClick = onClickRetry) {
                    Text(text = stringResource(R.string.camera_retry))
                }
                Button(onClick = onClickCancel) {
                    Text(text = stringResource(R.string.camera_cancel))
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSystemCameraScreen() = PreviewBox {
    SystemCameraScreen(
        state = SystemCameraState.RequestingPermission(),
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickRetry = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewSystemCameraScreenPermanentlyDenied() = PreviewBox {
    SystemCameraScreen(
        state = SystemCameraState.RequestingPermission(permanentlyDenied = true),
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        onClickRetry = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
