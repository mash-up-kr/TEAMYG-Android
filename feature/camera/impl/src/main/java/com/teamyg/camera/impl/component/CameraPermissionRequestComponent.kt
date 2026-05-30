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
    onClickGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.Black)
            .padding(PaddingValues(24.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "카메라 권한이 필요합니다.",
            color = Color.White,
        )
        Button(onClick = onClickGrantPermission) {
            Text(text = "권한 요청")
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraPermissionRequestComponent() = PreviewBox {
    CameraPermissionRequestComponent(
        onClickGrantPermission = {},
        modifier = Modifier.fillMaxSize(),
    )
}
