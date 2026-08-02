package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
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
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.padding.padding5),
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .padding(horizontal = YGTheme.layout.padding.padding7),
    ) {
        item(
            key = "header-recent",
            span = { GridItemSpan(maxLineSpan) },
        ) {
            GalleryDateHeader(date = "최근 업로드한 사진")
        }

        if (recentImages.isNotEmpty()) {
            items(
                items = recentImages,
                key = { "recent-$it" },
            ) { uri ->
                GalleryImageCell(
                    uri = uri,
                    onClickImage = onClickImage,
                )
            }
        } else {
            item(key = "recent-empty") {
                Box(modifier = Modifier.aspectRatio(1f))
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
        color = YGAtomicColors.Gray.Gray900,
        style = YGTheme.typography.body.b02R,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = YGTheme.layout.padding.padding5, top = YGTheme.layout.padding.padding8),
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
