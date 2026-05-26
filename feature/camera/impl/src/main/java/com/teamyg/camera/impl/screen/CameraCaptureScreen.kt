package com.teamyg.camera.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun CameraCaptureScreen(
    onClickConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Text(text = "커스텀 카메라 (구현 예정)")

        Button(
            onClick = onClickConfirm,
        ) {
            Text(text = "촬영 완료 (더미)")
        }
    }
}

@YGPreview
@Composable
private fun PreviewCameraCaptureScreen() = PreviewBox {
    CameraCaptureScreen(
        onClickConfirm = {},
        modifier = Modifier.fillMaxSize(),
    )
}
