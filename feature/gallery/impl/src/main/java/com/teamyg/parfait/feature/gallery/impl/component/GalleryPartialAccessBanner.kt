package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tjyg.core.ui.preview.PreviewBox
import com.tjyg.core.ui.preview.YGPreview

@Composable
internal fun GalleryPartialAccessBanner(
    onClickManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.DarkGray)
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "일부 사진만 접근할 수 있어요.",
            color = Color.White,
        )

        TextButton(onClick = onClickManage) {
            Text(
                text = "선택 관리",
                color = Color.White,
            )
        }
    }
}

@YGPreview
@Composable
private fun PreviewGalleryPartialAccessBanner() = PreviewBox {
    GalleryPartialAccessBanner(
        onClickManage = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
