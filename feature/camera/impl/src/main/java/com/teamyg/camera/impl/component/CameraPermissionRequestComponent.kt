package com.teamyg.camera.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun CameraPermissionRequestComponent(
    isInit: Boolean,
    permanentlyDenied: Boolean,
    onClickGrantPermission: () -> Unit,
    onClickOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(PaddingValues(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isInit) {
            when (permanentlyDenied) {
                true -> {
                    Text(
                        text = "카메라 권한이 거부되어 있습니다.\n설정에서 권한을 허용해주세요.",
                        color = Color.White,
                    )
                    Button(onClick = onClickOpenAppSettings) {
                        Text(text = "설정 열기")
                    }
                }

                false -> {
                    Text(
                        text = "카메라 권한이 필요합니다.",
                        color = Color.White,
                    )
                    Button(onClick = onClickGrantPermission) {
                        Text(text = "권한 요청")
                    }
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraPermissionRequestComponent() = PreviewBox {
    CameraPermissionRequestComponent(
        isInit = true,
        permanentlyDenied = false,
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        modifier = Modifier.fillMaxSize(),
    )
}

@YGPreview
@Composable
private fun PreviewCameraPermissionRequestComponentPermanentlyDenied() = PreviewBox {
    CameraPermissionRequestComponent(
        isInit = true,
        permanentlyDenied = true,
        onClickGrantPermission = {},
        onClickOpenAppSettings = {},
        modifier = Modifier.fillMaxSize(),
    )
}
