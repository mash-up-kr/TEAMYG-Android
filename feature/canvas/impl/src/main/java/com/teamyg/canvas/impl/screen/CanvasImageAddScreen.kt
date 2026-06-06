package com.teamyg.canvas.impl.screen

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
internal fun CanvasImageAddScreen(
    onClickCamera: () -> Unit,
    onClickGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text(text = "캔버스 이미지 추가")

        Button(
            onClick = onClickCamera,
        ) {
            Text(text = "카메라로 촬영")
        }

        Button(
            onClick = onClickGallery,
        ) {
            Text(text = "갤러리에서 선택")
        }
    }
}

@YGPreview
@Composable
private fun PreviewCanvasImageAddScreen() = PreviewBox {
    CanvasImageAddScreen(
        onClickCamera = {},
        onClickGallery = {},
        modifier = Modifier.fillMaxSize(),
    )
}
