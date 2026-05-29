package com.teamyg.camera.impl.screen

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
import androidx.compose.ui.unit.dp
import com.teamyg.camera.impl.vm.SystemCameraState
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun SystemCameraScreen(
    state: SystemCameraState,
    onClickGrantPermission: () -> Unit,
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
                Text(text = "카메라를 여는 중입니다…")
            }

            SystemCameraState.RequestingPermission -> {
                Text(text = "카메라 권한이 필요합니다.")
                Button(onClick = onClickGrantPermission) {
                    Text(text = "권한 요청")
                }
            }

            SystemCameraState.Capturing -> {
                CircularProgressIndicator()
                Text(text = "촬영 중…")
            }

            SystemCameraState.Failed -> {
                Text(text = "촬영에 실패했습니다.")
                Button(onClick = onClickRetry) {
                    Text(text = "다시 시도")
                }
                Button(onClick = onClickCancel) {
                    Text(text = "취소")
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewSystemCameraScreen() = PreviewBox {
    SystemCameraScreen(
        state = SystemCameraState.RequestingPermission,
        onClickGrantPermission = {},
        onClickRetry = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
