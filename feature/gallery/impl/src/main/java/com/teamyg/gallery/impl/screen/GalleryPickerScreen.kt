package com.teamyg.gallery.impl.screen

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
internal fun GalleryPickerScreen(
    modifier: Modifier = Modifier,
    onClickConfirm: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        Text(text = "커스텀 갤러리 (구현 예정)")
        Button(
            onClick = onClickConfirm,
        ) {
            Text(text = "선택 완료 (더미)")
        }
    }
}

@YGPreview
@Composable
private fun PreviewGalleryPickerScreen() = PreviewBox {
    GalleryPickerScreen(
        modifier = Modifier.fillMaxSize(),
        onClickConfirm = {},
    )
}
