package com.teamyg.parfait.feature.gallery.impl.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButton
import com.teamyg.parfait.core.designsystem.component.ygcirclebutton.YGCircleButtonType
import com.teamyg.parfait.core.designsystem.theme.YGTheme
import com.teamyg.parfait.core.designsystem.theme.colors.YGAtomicColors
import com.teamyg.parfait.feature.gallery.impl.component.GalleryImageGridComponent
import com.teamyg.parfait.feature.gallery.impl.component.GalleryPartialAccessBanner
import com.teamyg.parfait.feature.gallery.impl.component.GalleryPermissionRequestComponent
import com.teamyg.parfait.feature.gallery.impl.model.GalleryAccessLevelPreviewParameterProvider
import com.teamyg.parfait.feature.gallery.impl.viewmodel.CustomGalleryPickerState
import com.teamyg.parfait.core.designsystem.utils.preview.PreviewBox
import com.teamyg.parfait.core.designsystem.utils.preview.YGPreview
import com.teamyg.parfait.core.util.android.permission.GalleryPermissionManager
import com.teamyg.parfait.domain.model.GalleryImageGroup
import com.teamyg.parfait.core.designsystem.R as DesignSystemR

@Composable
internal fun CustomGalleryPickerScreen(
    state: CustomGalleryPickerState,
    onClickGrantPermission: () -> Unit,
    onClickOpenSettings: () -> Unit,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.access.hasPermission) {
        true -> GalleryContent(
            isPartial = state.access.isPartial,
            isLoading = state.isLoading,
            isEmpty = state.isEmpty,
            groups = state.groups,
            recentImages = state.recentImages,
            onClickManageMedia = onClickManageMedia,
            onClickImage = onClickImage,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )

        false -> GalleryPermissionRequestComponent(
            isDeniedPermission = state.access.isDeniedPermission,
            onClickGrantPermission = onClickGrantPermission,
            onClickOpenSettings = onClickOpenSettings,
            onClickCancel = onClickCancel,
            modifier = modifier,
        )
    }
}

@Composable
private fun GalleryContent(
    isPartial: Boolean,
    isLoading: Boolean,
    isEmpty: Boolean,
    groups: List<GalleryImageGroup>,
    recentImages: List<String>,
    onClickManageMedia: () -> Unit,
    onClickImage: (String) -> Unit,
    onClickCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(YGAtomicColors.Gray.White)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(
                    start = YGTheme.layout.padding.padding7,
                    end = YGTheme.layout.padding.padding7,
                    top = YGTheme.layout.padding.padding6,
                ),
            horizontalArrangement = Arrangement.End,
        ) {
            YGCircleButton(
                iconResource = DesignSystemR.drawable.ic_close,
                type = YGCircleButtonType.Default,
                contentDescription = null,
                onClick = onClickCancel,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YGTheme.layout.gap.gap1, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(DesignSystemR.drawable.image_gallery_empty),
                    contentDescription = null,
                )
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = Color.White)

                    isEmpty -> Text(
                        text = "오늘 찍은 사진이 없어요\n" +
                            "사진을 찍고 토핑을 추가해 보세요",
                        color = YGAtomicColors.Gray.Gray300,
                        style = YGTheme.typography.body.b02R,
                        textAlign = TextAlign.Center
                    )

                    else -> GalleryImageGridComponent(
                        groups = groups,
                        recentImages = recentImages,
                        onClickImage = onClickImage,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@YGPreview
@Composable
private fun PreviewCustomGalleryPickerScreen(
    @PreviewParameter(GalleryAccessLevelPreviewParameterProvider::class)
    access: GalleryPermissionManager.GalleryAccessLevel,
) = PreviewBox {
    CustomGalleryPickerScreen(
        state = CustomGalleryPickerState(
            access = access,
        ),
        onClickGrantPermission = {},
        onClickOpenSettings = {},
        onClickManageMedia = {},
        onClickImage = {},
        onClickCancel = {},
        modifier = Modifier.fillMaxSize(),
    )
}
