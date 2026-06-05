package com.teamyg.gallery.impl.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.gallery.impl.viewmodel.SystemGalleryState
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun SystemGalleryPickerScreen(
    state: SystemGalleryState,
    onClickConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceAround,
    ) {
        AsyncImage(
            model = state.imageUri,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(150.dp),
        )
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
private fun PreviewSystemGalleryPickerScreen() =
    PreviewBox {
        SystemGalleryPickerScreen(
            state = SystemGalleryState(),
            onClickConfirm = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
