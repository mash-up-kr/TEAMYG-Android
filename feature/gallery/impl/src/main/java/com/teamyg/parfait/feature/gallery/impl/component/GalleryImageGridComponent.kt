package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.domain.model.GalleryImageGroup

@Composable
internal fun GalleryImageGridComponent(
    groups: List<GalleryImageGroup>,
    recentImages: List<String>,
    onClickImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(count = 3),
        modifier = modifier
            .background(Color.Black)
            .padding(horizontal = 2.dp),
    ) {
        if (recentImages.isNotEmpty()) {
            item(
                key = "header-recent",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GalleryDateHeader(date = "최근")
            }

            items(
                items = recentImages,
                key = { "recent-$it" },
            ) { uri ->
                GalleryImageCell(
                    uri = uri,
                    onClickImage = onClickImage,
                )
            }
        }

        groups.forEach { group ->
            item(
                key = "header-${group.date}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GalleryDateHeader(date = group.date)
            }

            items(
                items = group.images,
                key = { it },
            ) { uri ->
                GalleryImageCell(uri = uri, onClickImage = onClickImage)
            }
        }
    }
}

@Composable
private fun GalleryImageCell(
    uri: String,
    onClickImage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clickable { onClickImage(uri) },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun GalleryDateHeader(
    date: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = date,
        color = Color.White,
        style = MaterialTheme.typography.titleSmall,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 4.dp,
                vertical = 12.dp,
            ),
    )
}

@YGPreview
@Composable
private fun PreviewGalleryImageGridComponent() = PreviewBox {
    GalleryImageGridComponent(
        groups = listOf(
            GalleryImageGroup(
                date = "2023.07.01",
                images = listOf(
                    "test1",
                    "test2",
                ),
            ),
            GalleryImageGroup(
                date = "2023.07.02",
                images = listOf(
                    "test3",
                    "test4",
                ),
            ),
        ),
        recentImages = listOf(
            "test1",
            "test3",
        ),
        onClickImage = {},
    )
}
