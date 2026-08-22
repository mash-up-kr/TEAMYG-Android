package com.teamyg.parfait.feature.gallery.impl.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.clickable.clickableYGNoRipple
import com.teamyg.parfait.core.util.jvm.model.DateTextFormat
import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.domain.model.image.RecentImage
import com.teamyg.parfait.domain.model.image.RecentImageKind
import com.teamyg.parfait.feature.gallery.impl.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format

@Composable
internal fun GalleryImageGridComponent(
    groups: List<GalleryImageGroup>,
    recentImages: List<RecentImage>,
    onClickImage: (String) -> Unit,
    onClickCutoutImage: (RecentImage) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(count = 3),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.padding.padding5),
        modifier = modifier
            .background(YGAtomicColors.Gray.White)
            .padding(horizontal = YGTheme.layout.padding.padding7),
    ) {
        if (recentImages.isNotEmpty()) {
            item(
                key = "header-recent",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GalleryDateHeader(date = stringResource(R.string.gallery_recent_uploaded_header))
            }

            items(
                items = recentImages,
                key = { "recent-${it.uri}" },
            ) { image ->
                GalleryImageCell(
                    uri = image.uri,
                    // 알맹이는 투명 여백을 걷어낸 객체라 잘라 채우면 잘린다
                    contentScale = when (image.kind) {
                        RecentImageKind.SOURCE -> ContentScale.Crop
                        RecentImageKind.CUTOUT -> ContentScale.Fit
                    },
                    onClickImage = {
                        when (image.kind) {
                            RecentImageKind.SOURCE -> onClickImage(image.uri)
                            RecentImageKind.CUTOUT -> onClickCutoutImage(image)
                        }
                    },
                )
            }
        }

        groups.forEach { group ->
            item(
                key = "header-${group.date}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                GalleryDateHeader(
                    date = group.date.format(DateTextFormat.monthDayFormat),
                    dayOfWeek = group.date.format(DateTextFormat.weekdayFormat),
                )
            }

            items(
                items = group.images,
                key = { it },
            ) { uri ->
                GalleryImageCell(uri = uri, onClickImage = { onClickImage(uri) })
            }
        }
    }
}

@Composable
private fun GalleryImageCell(
    uri: String,
    onClickImage: () -> Unit,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Box(
        modifier = modifier
            .padding(bottom = YGTheme.layout.padding.padding5)
            .aspectRatio(1f)
            .clickableYGNoRipple { onClickImage() },
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = contentScale,
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
            .padding(bottom = YGTheme.layout.padding.padding5),
    )
}

@Composable
private fun GalleryDateHeader(
    date: String,
    dayOfWeek: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YGTheme.layout.padding.padding5),
        horizontalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1),
    ) {
        Text(
            text = date,
            color = YGAtomicColors.Gray.Gray900,
            style = YGTheme.typography.body.b02R,
        )
        Text(
            text = "($dayOfWeek)",
            color = YGAtomicColors.Gray.Gray300,
            style = YGTheme.typography.body.b02R,
        )
    }
}

@YGPreview
@Composable
private fun PreviewGalleryImageGridComponent() = PreviewBox {
    GalleryImageGridComponent(
        groups = listOf(
            GalleryImageGroup(
                date = LocalDate(2023, 7, 1),
                images = listOf(
                    "test1",
                    "test2",
                    "test3",
                    "test4",
                    "test5",
                ),
            ),
            GalleryImageGroup(
                date = LocalDate(2023, 7, 2),
                images = listOf(
                    "test6",
                    "test7",
                ),
            ),
        ),
        recentImages = listOf(
            RecentImage(uri = "test1", filePath = "test1", kind = RecentImageKind.SOURCE),
            RecentImage(uri = "test3", filePath = "test3", kind = RecentImageKind.SOURCE),
        ),
        onClickImage = {},
        onClickCutoutImage = {},
    )
}
